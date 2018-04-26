package Net;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Server {

   private String ServerIP;
   private int portNumber;
   private DatagramSocket socket = null;
   private String state = "none";//changes based on the state of the communication!!!
   private int SYNC_NUM = 0,sequence_num=0;
   private int defaultPayload =1024;
   private int maxPayload;
   private boolean hasValue = false;
   private int i = 1;

    private void initialize(String args[]) throws IOException {//initializes the server..asks for ip and port
        System.out.println("Server started!!!");
        if(args.length == 2){
            ServerIP = args[0];
            portNumber = Integer.parseInt(args[1]);

        }else if(args.length == 0){
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Give Server IP: ");
            ServerIP = br.readLine();
            System.out.println("Give Port Number: ");
            portNumber = Integer.parseInt(br.readLine());

        }else{
            System.out.println("Wrong arguments!!!");
        }
        communicationHandler(state);

    }

    /*
    It will handle the communication with the client..receives a packet and waits for a certain amount of time
    before it sents back an ack.
     */
    private void communicationHandler(String state){
        try {
            socket = new DatagramSocket(portNumber);
        }catch (SocketException e) {
            e.printStackTrace();
        }
        FileOutputStream file = null;

        while(true){
            byte[] buf = new byte[defaultPayload];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            System.out.println("Waiting for a request!!!");
            try {
                socket.receive(packet);
                System.out.println("Got a request!!!");
                Packet p = Packet.processingData(new String(buf));
                if(!hasValue) {
                    maxPayload = Integer.parseInt(p.getData());//we received the payload that it will be used in
                    hasValue = true;                          //the communication
                }
                InetAddress clientAddress = packet.getAddress();//gets the address and the port of the client.
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
                        try {
                            file = new FileOutputStream("FTPresult" + i);
                        }catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }else if(state.equals("established")){
                    //if packet is valid..opening the file and converting it to byte array so we can send it to client.
                    boolean completed = false;
                    while(!completed) {
                        byte[] buffer = new byte[maxPayload];
                        DatagramPacket packet1 = new DatagramPacket(buffer, buffer.length);
                        try {
                            socket.setSoTimeout(0);
                            socket.receive(packet1);
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(packet1.getData());
                            DataInputStream in = new DataInputStream(inputStream);
                            int sequence_number = in.readInt();
                            if(sequence_number == sequence_num){
                                int packetLength = in.readInt();
                                byte[] data = new byte[packetLength];
                                in.read(data);
                                file.write(data);
                                causeDelay();
                                sendAck(sequence_number,clientAddress,clientPort);
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
                                causeDelay();
                                sendAck(sequence_number,clientAddress,clientPort);
                                completed = true;
                                in.close();
                                inputStream.close();
                                file.close();
                                completed = true;
                                hasValue = false;
                                state="none";
                                i++;
                                sequence_num = 0;
                            }else{
                                System.out.println("Packet is a duplicate...just send ack!!!");
                                causeDelay();
                                sendAck(sequence_number,clientAddress,clientPort);
                            }
                        } catch (IOException | InterruptedException e) {
                             e.printStackTrace();

                        }
                    }

                }
            } catch (IOException e) {
                System.out.println("No requests have been made...");
            }
        }

    }

    private void sendAck(int ackNum,InetAddress address,int clientPort) throws IOException {
        Packet ack = new Packet();
        ack.setAckPacket(true);
        ack.setAckNum(ackNum);
        sendData(ack.toString(),address,clientPort);
    }


    private void sendData(String data,InetAddress address,int clientPort) throws IOException {
        byte[] buffer = data.getBytes();
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,clientPort);
        socket.send(p);
    }

    private void causeDelay() throws InterruptedException {
      double timeout = getTimeout();
      long timeoutTime = (long) timeout;
      Thread.sleep(timeoutTime);
    }

    private double getTimeout() {
      Random rand = new Random();
      double lambda = 1/4.2;
      return  Math.log(1-rand.nextDouble())/(-lambda);
    }


    public static void main(String args[]) throws IOException {
      new Server().initialize(args);
    }


}
