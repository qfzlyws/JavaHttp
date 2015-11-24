
/*************************************
 * 一个基础的代理服务器类
 *************************************
 */
import java.net.*;
import java.nio.CharBuffer;
import java.util.ArrayList;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.print.attribute.standard.MediaSize.ISO;

import java.io.*;
import java.lang.reflect.Constructor;

public class HttpProxy extends Thread {
	static public int CONNECT_RETRIES = 5; // 嘗試連接服務器的次數
	static public int CONNECT_PAUSE = 5;
	static public int TIMEOUT = 1000;
	static public int BUFSIZ = 1024 * 1024;
	static public boolean logging = false;
	static public OutputStream log = null;
	static public URL url = null;

	// 传入数据用的Socket
	protected Socket socket;
	// 上级代理服务器，可选
	static private String parent = null;
	static private int parentPort = -1;

	static public void setParentProxy(String name, int pport) {
		parent = name;
		parentPort = pport;
	}

	// 在给定Socket上创建一个代理线程。
	public HttpProxy(Socket s) {
		socket = s;
		start();
	}

	public void writeLog(int c, boolean browser) throws IOException {
		log.write(c);
	}

	public void writeLog(byte[] bytes, int offset, int len, boolean browser) throws IOException {
		for (int i = 0; i < len; i++)
			writeLog((int) bytes[offset + i], browser);
	}

	@SuppressWarnings("resource")
	public void run() {
		String host = "";
		int port = 80;
		Socket outbound = null; // Server socket

		try {
			InputStream is = socket.getInputStream(); // Client input stream
			OutputStream os = null;
			String requestFirstLine = "";
			String tmp = "";

			socket.setSoTimeout(TIMEOUT);

			BufferedReader bf = new BufferedReader(new InputStreamReader(is));

			// 读取請求頭的第一行內容
			requestFirstLine = bf.readLine();

			System.out.println("客戶端請求頭:" + requestFirstLine);

			tmp = requestFirstLine.substring(requestFirstLine.indexOf(" ") + 1);

			// 獲取URL地址
			if (tmp.indexOf(":443") != -1)
				tmp = "https://" + tmp;

			url = new URL(tmp.substring(0, tmp.indexOf(" ")));

			host = url.getHost();

			if (url.getPort() != -1)
				port = url.getPort();
			/*
			 * host = tmp.substring(0, tmp.indexOf(" "));
			 * 
			 * host = host.replaceAll("http://", ""); host =
			 * host.replaceAll("https://", "");
			 * 
			 * if (host.indexOf("/") != -1) host = host.substring(0,
			 * host.indexOf("/"));
			 * 
			 * if (tmp.indexOf("//") != -1) { tmp =
			 * tmp.substring(tmp.indexOf(":") + 1); }
			 * 
			 * // 判斷是否含有端口號 if (tmp.indexOf(":") != -1) { tmp =
			 * tmp.substring(tmp.indexOf(":") + 1); port =
			 * Integer.parseInt(tmp.substring(tmp.indexOf(":") + 1, tmp.indexOf(
			 * " "))); host = host.substring(0, host.indexOf(":")); }
			 */

			if (host != "") {
				if (parent != null) {
					host = parent;
					port = parentPort;
				}

				int retry = CONNECT_RETRIES;
				while (retry-- != 0) {
					try {

						if (requestFirstLine.indexOf("CONNECT") != -1)
							outbound = SSLSocketFactory.getDefault().createSocket(host, port);
						else
							outbound = new Socket(host, port);

						break;
					} catch (Exception e) {
						e.getMessage();
					}

					Thread.sleep(CONNECT_PAUSE);
				}

				if (outbound != null) {
					outbound.setSoTimeout(TIMEOUT);
					os = outbound.getOutputStream();

					os.write(requestFirstLine.getBytes());

					pipe(is, outbound.getInputStream(), os, socket.getOutputStream());
				}

			}

		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				socket.close();
				outbound.close();
			} catch (Exception e2) {
				System.out.println(e2.getMessage());
			}
		}

	}

	/**
	 * @param is0
	 *            客戶端輸入流
	 * @param is1
	 *            服務器端輸入流
	 * @param os0
	 *            服務器端輸出流
	 * @param os1
	 *            客戶端輸出流
	 */
	void pipe(InputStream is0, InputStream is1, OutputStream os0, OutputStream os1) throws Exception {
		try {
			int ir;
			byte bytes[] = new byte[BUFSIZ];

			try {
				do {
					ir = is0.read(bytes);
					os0.write(bytes, 0, ir);
				} while (ir > 0);
			} catch (IOException e) {
				System.out.println(e);
			}

			try {
				do {
					ir = is1.read(bytes);
					os1.write(bytes, 0, ir);
				} while (ir > 0);
			} catch (IOException e) {
				System.out.println(e);
			}

		} catch (Exception e0) {
			System.out.println("Pipe異常: " + e0);
		}
	}

	static public void startProxy(int port, Class class1) {
		ServerSocket ssock;
		try {
			ssock = new ServerSocket(port);
			while (true) {
				Class[] sarg = new Class[1];
				Object[] arg = new Object[1];
				sarg[0] = Socket.class;
				try {
					Constructor cons = class1.getDeclaredConstructor(sarg);
					arg[0] = ssock.accept();
					cons.newInstance(arg); // 创建HttpProxy或其派生类的实例
				} catch (Exception e) {
					System.out.println(e.getMessage());
					Socket esock = (Socket) arg[0];
					try {
						esock.close();
					} catch (Exception ec) {
						System.out.println(ec.getMessage());
					}
				}
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	// 测试用的简单main方法
	static public void main(String args[]) {
		System.out.println("在端口808启动代理服务器\n");
		HttpProxy.log = System.out; // 默认情况下，日志信息输出到标准输出设备
		HttpProxy.logging = false;
		HttpProxy.startProxy(808, HttpProxy.class);
	}
}
