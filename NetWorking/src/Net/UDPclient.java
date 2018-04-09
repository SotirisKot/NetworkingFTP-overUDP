package Net;

import java.io.*;
import java.net.*;
import java.util.*;

public class UDPclient{

  private String serverIP;
  private int portNumber;
  private String fileName;
  private String filePath;
  private int maxPayload;
  private int ACK_NUM = 0;
  private int SYNC_NUM = 0;
  private InetAddress address;
  private DatagramSocket socket;
  private String state = null;
  private int sequence_num = 0;

  public void initialize(String args[]) throws IOException {
      if(args.length != 5){
          System.out.println("Wrong Arguments!!!");
      }else{
          serverIP = args[0];
          portNumber = Integer.parseInt(args[1]);
          fileName = args[1];
          filePath = args[2];
          maxPayload = Integer.parseInt(args[4]);
          //get a Datagram socket
          socket = new DatagramSocket();
          String message = "Hello Server";
          address = InetAddress.getByName(serverIP);

          //START MAIN PROCESS
          /*INITIALIZE 3-WAY HANDSHAKE BY SENDING A SYN PACKET*/
          Packet syncPacket = new Packet();
          syncPacket.setSynPacket(true);
          syncPacket.setSynNum(SYNC_NUM);
          sendData(syncPacket.toString());
          System.out.println("3-way handshake initialized.");
          state = "sync_sent";
          waitingForInput(state);
      }
  }

  public void sendData(String data) throws IOException {
        byte[] buffer = data.getBytes();
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,portNumber);
        socket.send(p);
  }

  private void waitingForInput(String state){
      Thread t = new Thread(()-> {
          while(true){
              byte[] buffer = new byte[maxPayload];
              DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
              try {
                  socket.receive(packet);
                  Packet p = Packet.processingData(new String(buffer));
                  System.out.println("Received: " + p.toString());
                  if(state.equals("sync_sent")){
                      if(p.getAckNum() == SYNC_NUM){//ack packet must be valid!!!
                          System.out.println("3-way handshake step 2...Now will send ack back to server to establish" +
                                  " connection!!!");
                          Packet ackPacket = new Packet();
                          ackPacket.setAckPacket(true);
                          ackPacket.setAckNum(p.getSynNum());
                          sendData(ackPacket.toString());
                          System.out.println("3-way handshake finished...connection established.");
                      }
                  }
              } catch (IOException e) {
                    e.printStackTrace();
              }

          }
      });
      t.start();
  }

  public static void main(String args[]) throws IOException {
      new UDPclient().initialize(args);
  }


}
