package Net;


import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClientHandler extends Thread{
    private String filePath;
    private String fileName,extension;
    private DatagramSocket socket = null;
    private int portNumber;
     private InetAddress address;
    private String state = "none";
    private int SYNC_NUM = 0,sequence_num=0;
    private int maxPayload;
    private int defaultPayload = 1024;
    private int PacketCounter;

  public ClientHandler(InetAddress address, int portNumber, String filePath, String fileName, String extension, int maxPayload){
    this.address = address;
    this.portNumber = portNumber;
    this.filePath = filePath;
    this.fileName = fileName;
    this.extension = extension;
    this.maxPayload = maxPayload;
  }

  public void run() {
      Packet syncPacket = new Packet();
      syncPacket.setSynPacket(true);
      syncPacket.setSynNum(SYNC_NUM);
      syncPacket.setData(Integer.toString(maxPayload));
      try {
          socket = new DatagramSocket();
          sendData(syncPacket.toString());
      } catch (IOException e) {
          e.printStackTrace();
      }
      System.out.println("3-way handshake initialized.");
      state = "sync_sent";
      boolean complete = false;
      while (!complete) {
          byte[] buffer = new byte[defaultPayload];
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
          if (state.equals("sync_sent")) {
              try {
                  socket.receive(packet);
                  Packet p = Packet.processingData(new String(buffer));
                  System.out.println("Received: " + p.toString());
                  if (p.getAckNum() == SYNC_NUM) {//ack packet must be valid!!!
                      System.out.println("3-way handshake step 2...Now will send ack back to server to establish" +
                              " connection!!!");

                      sendAck(p.getSynNum());
                      System.out.println("3-way handshake finished...connection established.");
                      state = "connection_established";
                  }
              } catch (IOException e) {
                  e.printStackTrace();
              }
          } else if (state.equals("connection_established")) {

              Path path = Paths.get(filePath);
              try {
                  byte[] buf = Files.readAllBytes(path);
                  long startTime = System.nanoTime();
                  System.out.println(startTime + " NSEC");
                  transferFile(buf, address, portNumber);
                  long endTime = System.nanoTime();
                  System.out.println(endTime + " NSEC" + "\n diff " + (endTime - startTime));
                  getStatistics(startTime, endTime, PacketCounter, maxPayload);
                  complete = true;
              } catch (IOException e) {
                  e.printStackTrace();
              }

          }

      }
  }

  private void transferFile(byte[] buffer, InetAddress address, int portNumber) throws IOException {
      String state = "packet_send";
      int load = maxPayload-8;
      boolean file_sent = false;
      boolean last_packet = false;
      boolean send_again = false;
      int start=0;
      byte[] sendBuf = new byte[load];
      System.out.println("The file is: "+ buffer.length + " bytes!!");
      int timeout = 1500;
      System.out.println("The timeout will be: " + timeout + "ms");
      while (!file_sent){
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          DataOutputStream out = new DataOutputStream(stream);
          if(state.equals("packet_send")){
              if(!send_again){
                  for(int i=0; i<sendBuf.length; i++){
                      sendBuf[i] = buffer[start];
                      start++;
                      if(start == buffer.length){
                          last_packet = true;
                          break;
                      }
                  }
              }
              if(last_packet){
                  out.writeInt(2);
                  out.writeInt(load);
                  out.write(sendBuf);
                  out.flush();
                  byte[] buf = stream.toByteArray();
                  DatagramPacket packet = new DatagramPacket(buf,buf.length,address,portNumber);
                  socket.send(packet);
                  PacketCounter++;
                  state="wait_ack";
                  out.close();
                  stream.close();
              }else{
                  out.writeInt(sequence_num);
                  out.writeInt(load);
                  out.write(sendBuf);
                  out.flush();
                  byte[] buf = stream.toByteArray();
                  DatagramPacket packet = new DatagramPacket(buf,buf.length,address,portNumber);
                  socket.send(packet);
                  PacketCounter++;
                  state="wait_ack";
                  out.close();
                  stream.close();
              }
          }else if(state.equals("wait_ack")){
              byte[] bufferReceive = new byte[defaultPayload];
              DatagramPacket packetAck = new DatagramPacket(bufferReceive,bufferReceive.length);
              try{
                  socket.setSoTimeout(timeout);
                  socket.receive(packetAck);
                  Packet p = Packet.processingData(new String(bufferReceive));

                  if(p.getAckPacket() && p.getAckNum() == sequence_num){
                      if(sequence_num == 1){
                          sequence_num--;
                      }else if(sequence_num == 0){
                          sequence_num++;
                      }
                      state="packet_send";
                      send_again = false;
                  }else if(p.getAckPacket() && p.getAckNum() == 2){
                      System.out.println("Received ack for last packet!!!");
                      System.out.println("Total packets sent: " + PacketCounter);
                      file_sent = true;
                  }else{
                      System.out.println("Duplicate ack..Ignoring it!!!");
                      state = "wait_ack";
                  }
              }catch (IOException e){
                  System.out.println("Timed out...must send packet again!!!");
                  state="packet_send";
                  send_again = true;
              }
          }
      }
  }


    private void sendData(String data) throws IOException {
        byte[] buffer = data.getBytes();
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,portNumber);
        socket.send(p);
    }

    private void getStatistics(long startTime,long endTime,int PacketCounter,int maxPayload){
        long totalBytes = PacketCounter*maxPayload;
        long totalTime = endTime - startTime;
        double nanoTosec = totalTime*1e-9;
        double transferRate = (totalBytes/nanoTosec)/1024;
        System.out.println("Total transfer time: " + 1e-9 * totalTime + " seconds");
        System.out.println("Transfer rate: " +  transferRate + " Kbyte/sec");
        System.out.println("Total number of UDP/IP packets received: " + PacketCounter);
        System.out.println("The payload was: "+ maxPayload);
    }

    private void sendAck(int ackNum) throws IOException {
        Packet ack = new Packet();
        ack.setAckPacket(true);
        ack.setAckNum(ackNum);
        sendData(ack.toString());
    }
}
