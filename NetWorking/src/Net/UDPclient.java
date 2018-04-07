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

  public void initialize(String args[]) throws IOException {
      if(args.length != 5){
          System.out.println("Wrong Arguments!!!");
      }else{
          serverIP = args[0];
          portNumber = Integer.parseInt(args[1]);
          fileName = args[1];
          filePath = args[2];
          maxPayload = Integer.parseInt(args[4]);
      }
      //get a Datagram socket
      DatagramSocket socket = new DatagramSocket();

      //create a packet to send request
      byte[] buf = new byte[maxPayload];
      InetAddress address = InetAddress.getByName(serverIP);
      DatagramPacket packet = new DatagramPacket(buf, buf.length, address, portNumber);
      socket.send(packet);



  }

  public static void main(String args[]) throws IOException {
      new UDPclient().initialize(args);
  }


}
