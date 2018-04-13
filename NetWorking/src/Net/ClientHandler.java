package Net;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClientHandler extends Thread{
  private DatagramSocket socket = null;
  private int portNumber;
  private String ServerIP;
  private String state = "none";
  private int SYNC_NUM = 0,sequence_num=0;
  private int maxPayload = 65000;

  public ClientHandler(String ServerIP,int portNumber){
    this.ServerIP = ServerIP;
    this.portNumber = portNumber;
  }

  public void run(){
      try {
          socket = new DatagramSocket(portNumber);
      }catch (SocketException e) {
          e.printStackTrace();
      }
      while(true){
          byte[] buf = new byte[maxPayload];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          System.out.println("Waiting for a request!!!");
          try {
              socket.receive(packet);
              System.out.println("Got a request!!!");
              Packet p = Packet.processingData(new String(buf));
              InetAddress clientAddress = packet.getAddress();
              int clientPort = packet.getPort();
              if(state.equals("none")){//we wait for a syn packet..to start the handshake...
                  if(p.getSynPacket()){
                      System.out.println("Client initialized 3-way handshake!!!");
                      Packet response = new Packet();
                      response.setSynPacket(true);
                      response.setAckPacket(true);
                      response.setSynNum(SYNC_NUM);
                      response.setAckNum(p.getSynNum());
                      sendData(response.toString(),clientAddress,clientPort);
                      state = "syn_received";
                      System.out.println("3-way handshake step 2: sent ack-syn packet waiting for ack...");
                  }
              }else if(state.equals("syn_received")){
                  if(p.getAckPacket() && p.getAckNum() == SYNC_NUM){
                      state = "established";
                      System.out.println("3-way handshake completed...CONNECTION ESTABLISHED!!!");
                  }
              }else if(state.equals("established")){
                  //if packet is valid..opening the file and converting it to byte array so we can send it to client.
                  if(p.isDataPacket() && p.getSequence_num() == sequence_num){//packet is valid
                      String filepath = p.getData();
                      System.out.println(filepath);
                      Path path = Paths.get(filepath);
                      byte[] buffer = Files.readAllBytes(path);
                      //now must send an ack..to tell the client that we received the file request.
                      sendAck(p.getSequence_num(),clientAddress,clientPort);
                      sequence_num++;
                      try {
                          System.out.println("Starting transferring file!!!");
                          transferFile(buffer,clientAddress,clientPort);
                          System.out.println("File has been transferred");
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
              }
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
  }


  public void transferFile(byte[] buffer, InetAddress address, int clientPort) throws IOException {
      String state = "packet_send";
      int load = maxPayload-8;
      boolean image_sent = false;
      int start=0;
      int end = load;
      while (!image_sent){
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          DataOutputStream out = new DataOutputStream(stream);
          if(state.equals("packet_send")){
              byte[] sendBuf = new byte[load];
              for(int i=0; i<sendBuf.length; i++){
                  sendBuf[i] = buffer[start];
                  start++;
                  if(start == buffer.length){
                      System.out.println(start);
                      image_sent = true;
                      break;
                  }
              }
              out.writeInt(sequence_num);
              out.writeInt(load);
              out.write(sendBuf);
              out.flush();
              byte[] buf = stream.toByteArray();
              DatagramPacket packet = new DatagramPacket(buf,buf.length,address,clientPort);
              socket.send(packet);
              System.out.println(state);
              state="wait_ack";
              out.close();
              stream.close();
          }else if(state.equals("wait_ack")){
              System.out.println(state);
              byte[] bufferReceive = new byte[maxPayload];
              DatagramPacket packet = new DatagramPacket(bufferReceive,bufferReceive.length);
              try{
                  socket.setSoTimeout(5000);
                  socket.receive(packet);
                  Packet p = Packet.processingData(new String(bufferReceive));
                  if(p.getAckPacket() && p.getAckNum() == sequence_num){
                      if(sequence_num == 1){
                          sequence_num--;
                      }else if(sequence_num == 0){
                          sequence_num++;
                      }
                      System.out.println("Received ack");
                      state="packet_send";
                  }
              }catch (IOException e){
                  System.out.println("Timed out...must send packet again!!!");
                  state="packet_send";
              }
          }
      }

  }


  public void sendAck(int ackNum,InetAddress address,int clientPort) throws IOException {
      Packet ack = new Packet();
      ack.setAckPacket(true);
      ack.setAckNum(ackNum);
      sendData(ack.toString(),address,clientPort);
  }

  
  public void sendData(String data,InetAddress address,int clientPort) throws IOException {
      byte[] buffer = data.getBytes();
      DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,clientPort);
      socket.send(p);
  }

}
