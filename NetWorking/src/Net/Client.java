package Net;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static java.lang.System.exit;

public class Client {


  public static void main(String args[]) throws IOException {

      String serverIP = "";
      int portNumber = 0;
      String fileName = "";
      String filePath = "";
      String extension = "";
      int maxPayload = 0;
      InetAddress address = InetAddress.getByName(serverIP);
      String finalPath = "";
      boolean input = false;

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
          System.out.print("Give payload (Give value between 1-65500): ");
          //To try mpike edw se periptwsh pou o xrhsths dwsei opoiadhpote allh timh ektos apo int
          try {
              maxPayload = Integer.parseInt(br.readLine());
          }catch(NumberFormatException ex){
              System.out.println("Wrong value! We support only integers");
              input = true;
          }

          while(maxPayload<1 || maxPayload>65500 || input == true){
              System.out.println("Wrong value for payload please give again!");
              System.out.println("Give payload (Give value between 1-65500): ");
              try {
                  maxPayload = Integer.parseInt(br.readLine());
                  input = false;
              }catch(NumberFormatException ex){
                  System.out.println("Wrong value! We support only integers");
              }
          }

          address = InetAddress.getByName(serverIP);

      }else if(args.length == 5){
          serverIP = args[0];
          portNumber = Integer.parseInt(args[1]);
          fileName = args[2];
          //extension = fileName.substring(fileName.indexOf("."),fileName.length());
          filePath = args[3];
          try {
              maxPayload = Integer.parseInt(args[4]);
          }catch(NumberFormatException ex){
              System.out.println("Wrong value! We support only integers");
              System.out.println("Give arguments again!");
              System.out.println("Exiting...");
              exit(0);
          }
          if(maxPayload>65500 || maxPayload<1){
              System.out.println("Payload out of bounds!");
              System.out.println("Give arguments again!");
              System.out.println("Exiting...");
              exit(0);
          }
          //get a Datagram socket
          address = InetAddress.getByName(serverIP);
          System.out.println(fileName + "   " + filePath);

      }else{
          System.out.println("Wrong number of arguments!");
      }

      //Kanoume concat to file name kai to file path tou arxeioy poy peirame!
      finalPath = filePath.concat(fileName);
      //Vlepoume to extension tou file pou dwsame ws input wste to file pou 8a exoume san output na exei to idio extension
      extension = fileName.substring(fileName.indexOf("."),fileName.length());

      ClientHandler clientHandler = new ClientHandler(address,portNumber,finalPath,fileName,extension,maxPayload);
      clientHandler.start();
  }
}


