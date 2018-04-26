package Net;


import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/*
* ClientHandler: Η κλάση αυτή είναι ένα thread που τρέχει η Client και διευκολύνει την
* επικοινωνία μεταξύ του client και του server και είναι η κλάση η οποία λαμβάνει και στέλνει πακέτα απο και προς τον client.
* */

public class ClientHandler extends Thread{
    private String filePath;
    private String fileName,extension;
    private DatagramSocket socket = null;
    private int portNumber;
     private InetAddress address;
    private String state = "none";
    private int SYNC_NUM = 0,sequence_num=0;
    private int maxPayload;
    private int defaultPayload = 1024; //έχουμε θέσει εμείς default μέγεθος του buffer που θα αποθηκεύει τα πακέτα ack,syn_ack,sync
    private int PacketCounter;

  public ClientHandler(InetAddress address, int portNumber, String filePath, String fileName, String extension, int maxPayload){
    this.address = address;
    this.portNumber = portNumber;
    this.filePath = filePath;
    this.fileName = fileName;
    this.extension = extension;
    this.maxPayload = maxPayload;
  }

//Ξεκινάει η εκτέλεση του thread
  public void run() {
      //Για να ξεκινήσει το 3 way handshake πρέπει ο client να στείλει ένα sync πακέτο προς τον server πράγμα που γίνεται παρακάτω
      Packet syncPacket = new Packet();
      syncPacket.setSynPacket(true);
      syncPacket.setSynNum(SYNC_NUM);
      syncPacket.setData(Integer.toString(maxPayload)); //με το ack πακέτο περνάμε στον server το maxPayload που έσδωσε ο χρήστης
      try {
          socket = new DatagramSocket();
          sendData(syncPacket.toString());
      } catch (IOException e) {
          e.printStackTrace();
      }
      System.out.println("3-way handshake initialized.");
      state = "sync_sent";
      boolean complete = false;
      while (!complete) {
          byte[] buffer = new byte[defaultPayload];
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
          if (state.equals("sync_sent")) {
              try {
                  socket.receive(packet);
                  Packet p = Packet.processingData(new String(buffer));//Κάνουμε process τα στοιχεία του πακέτου που μόλις παραλάβαμε
                  System.out.println("Received: " + p.toString());
                  if (p.getAckNum() == SYNC_NUM) {//ack packet must be valid!!!
                      System.out.println("3-way handshake step 2...Now will send ack back to server to establish" +
                              " connection!!!");

                      sendAck(p.getSynNum());//Έχουμε παραλάβει ack_sync και στέλνουμε ack
                      System.out.println("3-way handshake finished...connection established.");
                      state = "connection_established";//το connection εγκαθιδρύθηκε 3 way handshake ολοκληρώθηκε
                  }
              } catch (IOException e) {
                  e.printStackTrace();
              }
          } else if (state.equals("connection_established")) {
              //Αφού ολοκληρώθηκε το handshake ξεκινάμε το process του αρχείου που μας έδωσε ο χρήστης σαν Input.
              Path path = Paths.get(filePath);
              try {
                  //Κάνουμε read όλα τα bytes απο το αρχείο μας
                  byte[] buf = Files.readAllBytes(path);
                  long startTime = System.nanoTime();
                  //Θέτουμε χρόνο εκκίνησης
                  System.out.println(startTime + " NSEC");
                  //Ξεκινάμε μεταφορά αρχείου
                  transferFile(buf, address, portNumber);
                  //θέτουμε χρόνο τέλους και μετά υπολογίζουμε το χρόνο που διήρκησε η μεταφορά του αρχείου
                  long endTime = System.nanoTime();
                  System.out.println(endTime + " NSEC" + "\n diff " + (endTime - startTime));
                  //Παράγουμε στατιστικά με βάση το χρόνο μεταφοράς αριθμό πακέτων που στάλθηκαν και το payload του χρήστη.
                  getStatistics(startTime, endTime, PacketCounter, maxPayload);
                  complete = true;
              } catch (IOException e) {
                  e.printStackTrace();
              }

          }

      }
  }
  //transferFile: Χρησιμοπιείται για την μεταφορά του αρχείου απο τον Client στον Server
  private void transferFile(byte[] buffer, InetAddress address, int portNumber) throws IOException {
      String state = "packet_send";
      //Θέτουμε το load να είναι maxPayload - 8 δίοτι έχω συνολικά 8 bytes ένα για το header και 7 για το data.
      // Άρα αν το payload είναι 65500 τότε θα έχω load = 65492.
      int load = maxPayload-8;
      boolean file_sent = false;
      boolean last_packet = false;
      boolean send_again = false;
      int start=0;
      byte[] sendBuf = new byte[load];
      System.out.println("The file is: "+ buffer.length + " bytes!!");
      //Θέτουμε timeout 1500 όπως έχει διευκρινιστεί απο την εκφώνηση της άσκησης
      int timeout = 1500;
      System.out.println("The timeout will be: " + timeout + "ms");
      //Ξεκινάει η μεταφορά του αρχείου όσο το file_sent = false
      while (!file_sent){
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          DataOutputStream out = new DataOutputStream(stream);
          //Σε περίπτωση που θέλω να στείλω κάποιο πακέτο βλέπω το state του. Αν το state είναι packet_send πάω και στέλνω πακέτο
          if(state.equals("packet_send")){
              if(!send_again){
                  //Αν το πακέτο που στέλνουμε δεν το έχουμε ξαναστείλει τότε θα ξεκινήσω να γεμίζω τον sendBuf μέσα απο τον
                  // buffer που περνάω στην μέθοδο ο οποίος περιέχει τα Bytes
                  //  του πακέτου που έδωσα. Χρησιμοποίουμε την μεταβλητή start για να ξέρω κάθε φορά απο ποιον αριθμό του πίνακα Buffer
                  // θα συνεχίσω να γεμίσω και το επόμενο sendBuf.
                  for(int i=0; i<sendBuf.length; i++){
                      sendBuf[i] = buffer[start];
                      start++;
                      if(start == buffer.length){
                          last_packet = true;
                          break;
                      }
                  }
              }
              //Άν είμαι στο τελευταίο πακέτο τότε θα γράψω στο datagramPacket που θα στείλω στον server,
              // τον αριθμό 2 έτσι ώστε να ξέρω ότι η μεταφορά του αρχείου ολοκληρώθηκε.
              if(last_packet){
                  out.writeInt(2);
                  out.writeInt(load);
                  out.write(sendBuf);
                  out.flush();
                  byte[] buf = stream.toByteArray();
                  DatagramPacket packet = new DatagramPacket(buf,buf.length,address,portNumber);
                  socket.send(packet);
                  PacketCounter++;
                  state="wait_ack";
                  out.close();
                  stream.close();
              }else{
                  //Σε περίπτωση που δεν είναι τελευταίο πακέτο το στέλνω κανονικά στον server περιμένοντας για το ack
                  out.writeInt(sequence_num);
                  out.writeInt(load);
                  out.write(sendBuf);
                  out.flush();
                  byte[] buf = stream.toByteArray();
                  DatagramPacket packet = new DatagramPacket(buf,buf.length,address,portNumber);
                  socket.send(packet);
                  PacketCounter++;
                  state="wait_ack";
                  out.close();
                  stream.close();
              }
              //Σε περίπτωση που έχω ήδη στείλει το πακέτο και περιμένω να λάβω ack, θέτω timeout και κατόπιν περιμένω να λάβω το ack.
              // Αν το λάβω τότε προχωράω στην αποστολή επόμενου πακέτου αλλίως αν δεν το λάβω προκαλείται timeout και πρέπει
              // να ξαναστείλω το ack του πακέτου.
          }else if(state.equals("wait_ack")){

              byte[] bufferReceive = new byte[defaultPayload];
              DatagramPacket packetAck = new DatagramPacket(bufferReceive,bufferReceive.length);
              try{
                  socket.setSoTimeout(timeout);
                  socket.receive(packetAck);
                  Packet p = Packet.processingData(new String(bufferReceive));
                  if(p.getAckPacket() && p.getAckNum() == sequence_num){
                      if(sequence_num == 1){
                          sequence_num--;
                      }else if(sequence_num == 0){
                          sequence_num++;
                      }
                      state="packet_send";
                      send_again = false;
                  }else if(p.getAckPacket() && p.getAckNum() == 2){
                      System.out.println("Received ack for last packet!!!");
                      System.out.println("Total packets sent: " + PacketCounter);
                      file_sent = true;
                  }else{
                      System.out.println("Duplicate ack..Ignoring it!!!");
                      state = "wait_ack";
                  }
              }catch (IOException e){
                  System.out.println("Timed out...must send packet again!!!");
                  state="packet_send";
                  send_again = true;
              }
          }
      }
  }

    //sendData: Την χρησιμοποιούμε προκειμένου να στείλουμε data στον Server μέσω ενός DatagramPacket
    private void sendData(String data) throws IOException {
        byte[] buffer = data.getBytes();
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,address,portNumber);
        socket.send(p);
    }

    //getStatistics: Παράγουμε στατιστικά για το σύνολο των πακέτων που μεταφέρθηκαν , το χρόνο που έκαναν να σταλούν και το payload του χρήστη
    private void getStatistics(long startTime,long endTime,int PacketCounter,int maxPayload){
        long totalBytes = PacketCounter*maxPayload;
        long totalTime = endTime - startTime;
        double nanoTosec = totalTime*1e-9;
        double transferRate = (totalBytes/nanoTosec)/1024;
        System.out.println("Total transfer time: " + 1e-9 * totalTime + " seconds");
        System.out.println("Transfer rate: " +  transferRate + " Kbyte/sec");
        System.out.println("Total number of UDP/IP packets received: " + PacketCounter);
        System.out.println("The payload was: "+ maxPayload);
    }

    //sendAck: Την χρησιμοποιούμε για να στείλουμε ack για το πακέτο που μόλις παραλάβαμε μέσω της sendData
    private void sendAck(int ackNum) throws IOException {
        Packet ack = new Packet();
        ack.setAckPacket(true);
        ack.setAckNum(ackNum);
        sendData(ack.toString());
    }
}
