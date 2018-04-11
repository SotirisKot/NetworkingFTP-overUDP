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
      if(args.length == 0){
          System.out.println("Wrong Arguments!!!");
          BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
          System.out.println("Give server ip: ");
          serverIP = br.readLine();
          System.out.println("Give Port number: ");
          portNumber = Integer.parseInt(br.readLine());
          System.out.println("Give file name");
          fileName = br.readLine();
          System.out.println("Give file path");
          filePath = br.readLine();
          System.out.println("Give payload");
          maxPayload = Integer.parseInt(br.readLine());

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



  private void communicationHandler(String state){

      Thread t = new Thread(()-> {
          String stateN = state;
          boolean asked = true;
          while(true){
              byte[] buffer = new byte[maxPayload];
              DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
              if(stateN.equals("sync_sent")){
                  try {
                      //byte[] buffer = new byte[maxPayload];
                      //DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
                      socket.receive(packet);
                      Packet p = Packet.processingData(new String(buffer));
                      System.out.println("Received: " + p.toString());
                      if(p.getAckNum() == SYNC_NUM){//ack packet must be valid!!!
                          System.out.println("3-way handshake step 2...Now will send ack back to server to establish" +
                                  " connection!!!");
                          sendAck(p.getSynNum());
                          System.out.println("3-way handshake finished...connection established.");
                          stateN = "connection_established";
                      }
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              }else if(stateN.equals("connection_established")){
                  //asking the server to send me the file
                  if(asked){
                      Packet req = new Packet();
                      req.setDataPacket(true);
                      req.setData(filePath);
                      req.setSequence_num(sequence_num);
                      boolean received = false;
                      while(!received){
                          try {
                              sendData(req.toString());
                              socket.setSoTimeout(5000);
                              socket.receive(packet);
                              Packet p = Packet.processingData(new String(buffer));
                              if(p.getAckPacket() && p.getAckNum() == sequence_num){
                                  received = true;
                                  sequence_num++;
                                  System.out.println("Request delivered");
                                  break;
                              }
                          } catch (IOException e) {
                              System.out.println("Timed out...must send packet again!!!");
                          }
                      }
                      asked = false;
                      System.out.println("Ready to accept file!!!");
                  }
              }
          }
      });
      t.start();
  }

  public void sendAck(int ackNum) throws IOException {
      Packet ack = new Packet();
      ack.setAckPacket(true);
      ack.setAckNum(ackNum);
      sendData(ack.toString());
  }

  public void sendData(String data) throws IOException {
      byte[] buffer = data.getBytes();
      DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,portNumber);
      socket.send(p);
  }

  public static void main(String args[]) throws IOException {
      new UDPclient().initialize(args);
  }


}
