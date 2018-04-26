package Net;

/*
* Packet class: Χρησιμοποιούμε αυτήν την κλάση προκειμένου να μπορέσουμε να μοντελοποιήσουμε τα πακέτα των sync, sync_ack,ack
* για να πραγματοποιηθεί σωστά το 3 way handshake.
* Κάθε αντικείμενο της κλάσης Packet διαθέτει 7 πεδία .Τα τρία πρώτα είναι
* boolean μεταβλητές οι οποίες μας ενημερώνουν για το αν πρόκειται για synPacket,ackPacket,dataPacket.
* Τα επόμενα 2 synNum,ackNum είναι για να μας βοηθήσουν όταν θα κάνουμε το πακέτο string μεταφράζοντας αυτες τις boolean μεταβλητες
* σε αριθμούς.
* Το data είναι η πληροφορία που μεταφέρει το κάθε πακέτο
* και τέλος το sequence number είναι ο αριθμός ακολουθίας του πακέτου που παίρνει τιμή είτε 0 είτε 1
*
* */


public class Packet {

    private boolean synPacket;
    private boolean ackPacket;
    private boolean dataPacket;
    private int synNum;
    private int ackNum;
    private String data;
    private int sequence_num;

    public Packet(){
        this.synPacket = false;
        this.ackPacket = false;
        this.synNum = 0;
        this.ackNum = 0;
        this.data = null;
        this.sequence_num = 0;
        this.dataPacket = false;
    }

    public Packet(boolean synPacket,boolean ackPacket,int synNum,int ackNum,String data,int sequence_num,
                  boolean dataPacket){

        this.synPacket = synPacket;
        this.ackPacket = ackPacket;
        this.synNum = synNum;
        this.ackNum = ackNum;
        this.data = data;
        this.sequence_num = sequence_num;
        this.dataPacket = dataPacket;
    }

    public boolean isDataPacket() {
        return dataPacket;
    }

    public int getSequence_num() {
        return sequence_num;
    }

    public boolean getSynPacket() {
        return synPacket;
    }

    public boolean getAckPacket() {
        return ackPacket;
    }

    public int getAckNum() {
        return ackNum;
    }

    public int getSynNum() {
        return synNum;
    }

    public String getData() {
        return data;
    }

    public void setAckPacket(boolean ackPacket) {
        this.ackPacket = ackPacket;
    }

    public void setSynPacket(boolean synPacket) {
        this.synPacket = synPacket;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public void setSynNum(int synNum) {
        this.synNum = synNum;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setSequence_num(int sequence_num) {
        this.sequence_num = sequence_num;
    }

    public void setDataPacket(boolean dataPacket) {
        this.dataPacket = dataPacket;
    }

    @Override
    public String toString(){//to send a packet we must convert it to a String first
        int syn,ack,dataN;
        if(synPacket){
            syn = 1;
        }else{
            syn = 0;
        }

        if(ackPacket){
            ack = 1;
        }else{
            ack = 0;
        }

        if(dataPacket){
            dataN=1;
        }else{
            dataN = 0;
        }

        int sNum = synNum;
        int aNum = ackNum;
        int seq_num = sequence_num;
        String dataT = data;

        String msg = "SYN=" + syn + "-" + "ACK=" + ack + "-" + "SYNn=" + sNum + "-" + "ACKn=" + aNum + "-" + "SEQ=" + seq_num +
                "-" + "Dpacket=" + dataN + "-"+ "DATA=" + dataT + "-";
        return msg;
    }

    /*
    * Αυτή η μέθοδος χρησιμοπιείται όταν λαβάνουμε ένα αντικείμενο τύπου packet
    * και θέλουμε να δούμε αναλόγα με τα στοιχεία που κουβαλάει για τι πακέτο πρόκειται και τι πληροροφορία μεταφέρει
    * */

    public static Packet processingData(String packetData){

        boolean synPacket;
        boolean ackPacket;
        int synNum;
        int ackNum;
        String data = "";

        String[] tokens = packetData.split("-");
        Packet packet = new Packet();
        for(String eachToken: tokens){
            String Tokens[] = eachToken.split("=");
            switch (Tokens[0]){
                case "SYN":
                    packet.setSynPacket(Tokens[1].equals("1"));
                    break;
                case "ACK":
                    packet.setAckPacket(Tokens[1].equals("1"));
                    break;
                case "SYNn":
                    packet.setSynNum(Integer.parseInt(Tokens[1]));
                    break;
                case "ACKn":
                    packet.setAckNum(Integer.parseInt(Tokens[1]));
                    break;
                case "DATA":
                    packet.setData(Tokens[1]);
                    break;
                case "SEQ":
                    packet.setSequence_num(Integer.parseInt(Tokens[1]));
                    break;
                case "Dpacket":
                    packet.setDataPacket(Tokens[1].equals("1"));
                    break;
            }
        }
        return packet;
    }
}
