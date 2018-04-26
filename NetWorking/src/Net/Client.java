package Net;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {


    public static void main(String args[]) throws IOException {

        String serverIP;
        int portNumber;
        String fileName;
        String filePath;
        String extension;
        int maxPayload;
        InetAddress address;

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
            address = InetAddress.getByName(serverIP);

        }else{
            serverIP = args[0];
            portNumber = Integer.parseInt(args[1]);
            fileName = args[2];
            extension = fileName.substring(fileName.indexOf("."),fileName.length());
            filePath = args[3];
            maxPayload = Integer.parseInt(args[4]);
            //get a Datagram socket
            address = InetAddress.getByName(serverIP);
            System.out.println(fileName + "   " + filePath);

        }

        ClientHandler clientHandler = new ClientHandler(address,portNumber,filePath,fileName,extension,maxPayload);
        clientHandler.start();
    }
}

