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
          byte[] buf = new byte[65000];
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
                      Path path = Paths.get(filepath);
                      byte[] buffer = Files.readAllBytes(path);

                      //now must send an ack..to tell the client that we received the file request.
                      /* TODO */

                      //afou steilei to ack apla 3ekinaei na stelnei to file se paketa.Me sequence_num = 1

                      /* TODO */

                  }

              }
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
  }

    //apla koimatai gia 5 sec kai kanontas join perimenw na teleiwsei kai meta kanw return true.
    public boolean timeout(int seconds) throws InterruptedException {
        Thread t1 = new Thread(()->{
            try {
                Thread.sleep(seconds*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.start();
        t1.join();
        return true;
    }

  public void sendData(String data,InetAddress address,int clientPort) throws IOException {
      byte[] buffer = data.getBytes();
      DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,clientPort);
      socket.send(p);
  }

}
