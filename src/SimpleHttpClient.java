import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * 一个简单的HTTP客户端，发送HTTP请求，模拟浏览器 可打印服务器发送过来的HTTP消息
 */
public class SimpleHttpClient {
	private static String encoding = "GBK";

	public static void main(String[] args) {
		try {
			Socket s = new Socket(InetAddress.getLocalHost(), 8080);
			OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());
			StringBuffer sb = new StringBuffer();
			sb.append("GET http://www.baidu.com/ HTTP/1.1\r\n");
			sb.append("Host: wwww.baidu.com\r\n");
			sb.append("Connection: Keep-Alive\r\n");
			// 注，这是关键的关键，忘了这里让我搞了半个小时。这里一定要一个回车换行，表示消息头完，不然服务器会等待
			sb.append("\r\n");
			osw.write(sb.toString());
			osw.flush();

			// --输出服务器传回的消息的头信息
			InputStream is = s.getInputStream();
			String line = null;
			int contentLength = 0;// 服务器发送回来的消息长度
			// 读取所有服务器发送过来的请求参数头部信息
			do {
				line = readLine(is, 0);
				// 如果有Content-Length消息头时取出
				if (line.startsWith("Content-Length")) {
					contentLength = Integer.parseInt(line.split(":")[1].trim());
				}
				// 打印请求部信息
				System.out.print(line);
				// 如果遇到了一个单独的回车换行，则表示请求头结束
			} while (!line.equals("\r\n"));

			// --输消息的体
			System.out.print(readLine(is, contentLength));

			// 关闭流
			is.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 这里我们自己模拟读取一行，因为如果使用API中的BufferedReader时，它是读取到一个回车换行后
	 * 才返回，否则如果没有读取，则一直阻塞，直接服务器超时自动关闭为止，如果此时还使用BufferedReader
	 * 来读时，因为读到最后一行时，最后一行后不会有回车换行符，所以就会等待。如果使用服务器发送回来的
	 * 消息头里的Content-Length来截取消息体，这样就不会阻塞
	 * 
	 * contentLe 参数 如果为0时，表示读头，读时我们还是一行一行的返回；如果不为0，表示读消息体，
	 * 时我们根据消息体的长度来读完消息体后，客户端自动关闭流，这样不用先到服务器超时来关闭。
	 */
	private static String readLine(InputStream is, int contentLe) throws IOException {
		ArrayList lineByteList = new ArrayList();
		byte readByte;
		int total = 0;
		if (contentLe != 0) {
			do {
				readByte = (byte) is.read();
				lineByteList.add(Byte.valueOf(readByte));
				total++;
			} while (total < contentLe);// 消息体读还未读完
		} else {
			do {
				readByte = (byte) is.read();
				lineByteList.add(Byte.valueOf(readByte));
			} while (readByte != 10);
		}

		byte[] tmpByteArr = new byte[lineByteList.size()];
		for (int i = 0; i < lineByteList.size(); i++) {
			tmpByteArr[i] = ((Byte) lineByteList.get(i)).byteValue();
		}
		lineByteList.clear();

		return new String(tmpByteArr, encoding);
	}
}