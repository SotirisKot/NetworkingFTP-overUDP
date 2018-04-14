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
  private int SYNC_NUM = 0;
  private InetAddress address;
  private DatagramSocket socket;
  private String state = null;
  private int sequence_num = 0;
  private String extension="";

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
          extension = fileName.substring(fileName.indexOf("."),fileName.length());
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
          extension = fileName.substring(fileName.indexOf("."),fileName.length());
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
          FileOutputStream file = null;
          int PacketCounter=0;
          try {
              file = new FileOutputStream("FTPresult".concat(extension));
          } catch (FileNotFoundException e) {
              e.printStackTrace();
          }
          boolean completed = false;
          while(!completed){
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
                  }else{
                      try {
                          long startTime = System.nanoTime();
                          socket.setSoTimeout(0);
                          socket.receive(packet);
                          PacketCounter++;
                          System.out.println("Received a packet...must send ack!!!");
                          ByteArrayInputStream inputStream = new ByteArrayInputStream(packet.getData());
                          DataInputStream in = new DataInputStream(inputStream);
                          int sequence_number = in.readInt();
                          if(sequence_number == sequence_num){
                              int packetLength = in.readInt();
                              byte[] data = new byte[packetLength];
                              in.read(data);
                              file.write(data);
                              sendAck(sequence_number);
                              if(sequence_num == 1){
                                  sequence_num--;
                              }else if(sequence_num == 0){
                                  sequence_num++;
                              }
                          }else if(sequence_number==2){
                              System.out.println("File has been transferred!!");
                              int packetLength = in.readInt();
                              byte[] data = new byte[packetLength];
                              in.read(data);
                              file.write(data);
                              sendAck(sequence_number);
                              completed = true;
                              in.close();
                              inputStream.close();
                              file.close();
                              long endTime = System.nanoTime();
                              getStatistics(startTime,endTime,PacketCounter,maxPayload);
                          }else{
                              System.out.println("Packet is a duplicate...just send ack!!!");
                              System.out.println(sequence_number);
                              sendAck(sequence_number);
                          }
                      } catch (IOException e) {
                          e.printStackTrace();

                      }

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

  public void getStatistics(long startTime,long endTime,int PacketCounter,int maxPayload){
      long totalTime = endTime - startTime;
      double transfer_rate = ((PacketCounter*maxPayload)/1000)/60;
      System.out.println("Total transfer rate: " + totalTime + " nanoseconds");
      System.out.println("Transfer rate: " + transfer_rate + " Kbyte/sec");
      System.out.println("Total number of UDP/IP packets received: " + PacketCounter);
      System.out.println("The payload was: "+ maxPayload);
  }


  public static void main(String args[]) throws IOException {
      new UDPclient().initialize(args);
  }


}
