package Net;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread{
  private DatagramSocket socket = null;
  private int portNumber;
  private String ServerIP;

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
          byte[] buf = new byte[256];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          System.out.println("Waiting for a request!!!");
          try {
              socket.receive(packet);
          } catch (IOException e) {
              e.printStackTrace();
          }
          System.out.println("Got a request!!!");
          String received = new String(packet.getData(), 0, packet.getLength());
          System.out.println("Message: " + received);

          String message = "Hello Client";
          buf = message.getBytes();
          InetAddress address = packet.getAddress();
          int port = packet.getPort();
          packet = new DatagramPacket(buf, buf.length, address, port);
          try {
              socket.send(packet);
          } catch (IOException e) {
              e.printStackTrace();
          }

      }
  }

}
