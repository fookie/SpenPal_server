package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import database.DatabaseManager;

public class SpenPalServer {

	public static void main(String[] args) throws Exception {

		SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) getSSLContext()
				.getServerSocketFactory();
		try {
			SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(12313);
			serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
			serverSocket.setUseClientMode(false);
			serverSocket.setWantClientAuth(true);
			if (!serverSocket.isClosed()) {
				System.out.println("Server runnning");
			}
			while (true) {
				SSLSocket socket = (SSLSocket) serverSocket.accept();

				Handler handler = new Handler(socket);
				handler.initiate();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static class Handler implements Runnable {
		private SSLSocket socket;

		public Handler(SSLSocket s) {
			socket = s;
		}

		DatabaseManager dm = new DatabaseManager("");

		public void initiate() {
			Thread thread = new Thread(this);
			thread.start();
		}

		@Override
		public void run() {
			try {
				DataInputStream dis = new DataInputStream(socket.getInputStream());
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				String request;
				while (socket.isConnected()) {

					System.out.println("----[" + socket.getInetAddress() + "]----");
					request = dis.readUTF();
					System.out.println(">> " + request);
					String[] commands = request.split(" ");
					try {
						switch (commands[0]) {
						case "101":// login
							String username = commands[1];
							String passwd = commands[2];
							// dos.writeUTF("success");
							if (dm.login(username, passwd)) {
								dos.writeUTF("success");
							} else {
								dos.writeUTF("failed");
							}
							break;
						case "100":
							String new_username = commands[1];
							String new_passwd = commands[2];
							// dos.writeUTF("success");
							if (dm.register(new_username, new_passwd)) {
								dos.writeUTF("success");
							} else {
								dos.writeUTF("failed");
							}
							break;
						/*
						 * case "130": String avt_username = commands[1]; File
						 * tosend = new File("./avatar/" + avt_username +
						 * ".png");
						 * 
						 * break;
						 */
						case "110":// insert user data
							String user = commands[1];
							String id = commands[2];
							String category = commands[3];
							String amount = commands[4];
							String date = commands[5];
							String comment = commands[6];
							String image = commands[7];

							String t = dm.insertUserdata_new(user, id, category, amount, date, comment, image);
							dos.writeUTF(t);
							break;
						case "111":// update userdata

							String duser = commands[1];
							String time = commands[2];
							System.out.println("want to update:" + duser + " " + time);
							dos.writeUTF(dm.updatecount(duser, time));
							System.out.println(dm.updatecount(duser, time) + " things");
							List l = dm.update_new(duser, time);
							Iterator it = l.iterator();
							while (it.hasNext()) {
								dos.writeUTF((String) it.next());
							}
							dos.writeUTF("end");
							dos.writeUTF(LocalDateTime.now().toString());
							break;

						case "120":// update friends's share

							String ur = commands[1];

							System.out.println("want to get friend's share:" + ur);
							List lt = dm.getfriendshare(ur);
							Iterator itt = lt.iterator();
							System.out.println("size:" + lt.size());
							dos.writeUTF(String.valueOf(lt.size()));
							while (itt.hasNext()) {
								dos.writeUTF((String) itt.next());
							}
							dos.writeUTF("end");
							dos.writeUTF(LocalDateTime.now().toString());
							break;

						case "119":// delete
							String dun = commands[1];
							String did = commands[2];
							String ddate = commands[3];
							dm.delUserdata(dun, did, ddate);
							break;
						case "121":// change share status
							String sun = commands[1];
							String sid = commands[2];
							String sdate = commands[3];
							dm.change_share_status(sun, sid, sdate, 1);
							break;

						case "200":// add friend
							String aduser = commands[1];
							String adf = commands[2];
							boolean result = dm.addFriend(aduser, adf);
							if (result) {
								dos.writeUTF("success");
							} else {
								dos.writeUTF("failed");
							}
							break;

						case "201":// get friendlist data
							String fuser = commands[1];
							List<String> li = dm.getFriends(fuser);
							Iterator<String> iti = li.iterator();
							dos.writeUTF(String.valueOf(li.size()));
							while (iti.hasNext()) {
								String row = iti.next();
								dos.writeUTF(row);
								System.out.println(row);
							}
							dos.writeUTF("end");
							break;

						case "202":// del friend
							String deluser = commands[1];
							String delf = commands[2];
							dm.deleteFriend(deluser, delf);
							break;

						case "524":// new AA payment
							String aaname = commands[1];
							float aapayment = Float.parseFloat(commands[2]);
							String aagroup = commands[3];
							if (dm.newAAPayment(aaname, aapayment, aagroup)) {
								System.out.println("New AA request handled");
							}
							List<Object> upd = dm.getAAResult(aaname, aagroup);
							dos.writeUTF(String.valueOf((upd.size() - 3) / 2 - 1) + " " + upd.get(0).toString() + " "
									+ upd.get(1).toString() + " " + upd.get(2).toString());
							System.out.println(String.valueOf(upd.size() / 3 - 2) + " " + upd.get(0).toString() + " "
									+ upd.get(1).toString() + " " + upd.get(2).toString());
							for (int i = 3; i < upd.size(); i += 2) {
								dos.writeUTF(String.valueOf((i - 3) / 2) + " " + upd.get(i).toString() + " "
										+ upd.get(i + 1).toString());
								System.out.println(String.valueOf((i - 3) / 2) + " " + upd.get(i).toString() + " "
										+ upd.get(i + 1).toString());
							}
							dos.flush();
							System.out.println("AA update");
							break;
						case "525":
							String rename = commands[1];
							String regroup = commands[2];
							List<Object> ref = dm.getAAResult(rename, regroup);
							/*
							 * send content: line 1: [quantity of lines except
							 * for this line] [average amount] [creditor
							 * position] [amount should pay] line 2~line n:
							 * [numbers of members(include user)] [name] [spend
							 * amount]
							 */
							System.out.println("list size: " + ref.size());
							dos.writeUTF(String.valueOf((ref.size() - 3) / 2 - 1) + " " + ref.get(0).toString() + " "
									+ ref.get(1).toString() + " " + ref.get(2).toString());
							System.out.println(String.valueOf(ref.size() / 3 - 2) + " " + ref.get(0).toString() + " "
									+ ref.get(1).toString() + " " + ref.get(2).toString());
							for (int i = 3; i < ref.size(); i += 2) {
								dos.writeUTF(String.valueOf((i - 3) / 2) + " " + ref.get(i).toString() + " "
										+ ref.get(i + 1).toString());
								System.out.println(String.valueOf((i - 3) / 2) + " " + ref.get(i).toString() + " "
										+ ref.get(i + 1).toString());
							}
							dos.flush();
							System.out.println("AA refreshed");
							break;
						case "526":
							String dename = commands[1];
							String degroup = commands[2];
							dm.quitAAGroup(dename, degroup);
							dos.flush();
							break;
						default:
							System.err.println("ERROR:wrong command: " + request);
							dos.writeUTF("error");
							break;
						}
					} catch (Exception e) {
						// e.printStackTrace();
						System.err.println("ERROR: " + e + " when dealing with " + request);

					}
				}
				System.out.println("closing..");
				dos.flush();
				dos.close();
				dis.close();
			} catch (IOException e) {
				System.out.println("disconnect: " + e);
			}
		}

		private boolean sendFile(File f) {
			try {
				if (socket.isConnected()) {
					DataInputStream data = new DataInputStream(new FileInputStream(f));
					DataOutputStream send = new DataOutputStream(socket.getOutputStream());
					byte[] b = new byte[1024];
					while (data.available() > 0) {
						data.readFully(b, 0, b.length);
						send.write(b);
					}
					send.flush();
					data.close();
					send.close();
				} else {
					return false;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}

	/**
	 * Setup SSLContext Reference:
	 * http://blog.csdn.net/a19881029/article/details/11742361
	 * 
	 * @return
	 * @throws Exception
	 */
	private static SSLContext getSSLContext() throws Exception {
		String protocol = "TLSV1";
		String serverCer = "./certificate/server.jks";
		String serverCerPwd = "storespanpal";
		String serverKeyPwd = "keyspanpal";

		// Key Store
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(serverCer), serverCerPwd.toCharArray());

		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(keyStore, serverKeyPwd.toCharArray());
		KeyManager[] kms = keyManagerFactory.getKeyManagers();

		TrustManager[] tms = null;
		SSLContext sslContext = SSLContext.getInstance(protocol);
		sslContext.init(kms, tms, null);

		return sslContext;
	}

}
