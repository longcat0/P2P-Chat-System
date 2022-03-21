package registry;
public class Peer {
	public String address;
	public int port;
	String teamName;
	
	Peer[] peersSent = null;
	
	String key() {
		return teamName;
	}
	public String toString() {
		return key() + " " + address + ":" + port;
	}
}