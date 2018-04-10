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
          fileName = args[2];
          filePath = args[3];
          maxPayload = Integer.parseInt(args[4]);
          //get a Datagram socket
          socket = new DatagramSocket();
          address = InetAddress.getByName(serverIP);
          System.out.println(fileName + "   " + filePath);
          //START MAIN PROCESS
          /*INITIALIZE 3-WAY HANDSHAKE BY SENDING A SYN PACKET*/
          Packet syncPacket = new Packet();
          syncPacket.setSynPacket(true);
          syncPacket.setSynNum(SYNC_NUM);
          sendData(syncPacket.toString());
          System.out.println("3-way handshake initialized.");
          state = "sync_sent";
          communicationHandler(state);
      }
  }

  public void sendData(String data) throws IOException {
        byte[] buffer = data.getBytes();
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,portNumber);
        socket.send(p);
  }

  private void communicationHandler(String state){
      Thread t = new Thread(()-> {
          String stateN = state;
          while(true){
              byte[] buffer = new byte[maxPayload];
              DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
              if(stateN.equals("sync_sent")){
                  try {
                      socket.receive(packet);
                      Packet p = Packet.processingData(new String(buffer));
                      System.out.println("Received: " + p.toString());
                      if(p.getAckNum() == SYNC_NUM){//ack packet must be valid!!!
                          System.out.println("3-way handshake step 2...Now will send ack back to server to establish" +
                                  " connection!!!");
                          Packet ackPacket = new Packet();
                          ackPacket.setAckPacket(true);
                          ackPacket.setAckNum(p.getSynNum());
                          sendData(ackPacket.toString());
                          System.out.println("3-way handshake finished...connection established.");
                          stateN = "connection_established";
                      }
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              }else if(stateN.equals("connection_established")){
                  //asking the server to send me the file
                  Packet req = new Packet();
                  req.setDataPacket(true);
                  req.setData(filePath);
                  req.setSequence_num(sequence_num);
                  try {
                      //after sending the request...must wait for an ack...or else if time runs out..sending again.

                      sendData(req.toString());
                      while (!timeout(5)){//trexei to thread kai meta apo 5 sec bgainei apo to loop.
                         socket.receive(packet);

                         //an labei to ack me acknum=sequence_num=0 tote eimaste komple kai apla perimenei
                          //na tou er8ei to prwto paketo apo to file me sequence_num = 1.
                      }
                      System.out.println("timed out");
                  } catch (IOException | InterruptedException e) {
                      e.printStackTrace();
                  }


              }
          }
      });
      t.start();
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

  public static void main(String args[]) throws IOException {
      new UDPclient().initialize(args);
  }


}
