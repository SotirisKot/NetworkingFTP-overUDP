package Net;

public class Packet {

    private boolean synPacket;
    private boolean ackPacket;
    private int synNum;
    private int ackNum;
    private String data;

    public Packet(){
        this.synPacket = false;
        this.ackPacket = false;
        this.synNum = 0;
        this.ackNum = 0;
        this.data = null;
    }

    public Packet(boolean synPacket,boolean ackPacket,int synNum,int ackNum,String data){
        this.synPacket = synPacket;
        this.ackPacket = ackPacket;
        this.synNum = synNum;
        this.ackNum = ackNum;
        this.data = data;
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

    @Override
    public String toString(){//to send a packet we must convert it to a String first
        int syn,ack;
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

        int sNum = synNum;
        int aNum = ackNum;
        String dataT = data;

        String msg = "SYN=" + syn + "-" + "ACK=" + ack + "-" + "SYNn" + sNum + "-" + "ACKn" + aNum + "-" + "DATA" + dataT;

        return msg;
    }

    public Packet processingData(String packetData){
        return null;
    }
}
