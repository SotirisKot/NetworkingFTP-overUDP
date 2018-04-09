package Net;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread{
  private DatagramSocket socket = null;
  private int portNumber;
  private String ServerIP;
  private String state = "none";
  private int SYNC_NUM = 0;

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
          byte[] buf = new byte[512];
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
              }
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
  }


    public void sendData(String data,InetAddress address,int clientPort) throws IOException {
        byte[] buffer = data.getBytes();
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,clientPort);
        socket.send(p);
    }

}
