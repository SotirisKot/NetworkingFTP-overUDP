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
  private static int ACK_NUM = 0;
  private static int SYNC_NUM = 0;
  private InetAddress address;
  private DatagramSocket socket;
  private String state = null;

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
          //create a packet to send request
         /* byte[] buf = message.getBytes();
          InetAddress address = InetAddress.getByName(serverIP);
          DatagramPacket packet = new DatagramPacket(buf, buf.length, address, portNumber);
          socket.send(packet);

          //gets a response from the server
          packet = new DatagramPacket(buf, buf.length);
          socket.receive(packet);
          String received = new String(packet.getData(), 0, packet.getLength());
          System.out.println("Message: " + received);*/

          //START MAIN PROCESS
          /*INITIALIZE THREE WAY HANDSHAKE BY SENDING A SYN PACKET*/
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
