package com.email;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

/**
 * 简易邮件发送<br>
 * 需提供邮件服务提供商的smtp地址、邮箱账号、密码收件人邮箱<br><br>
 * 使用方式:<br>
 * 1.实例化JMail对象，适合多次利用相同账号信息发送到相同收件人<br>
 * &nbsp;&nbsp; JMail jMail = new JMail(...);<br>
 * &nbsp;&nbsp; jMail.sendMail(subject, content);<br>
 * 2.直接调用JMail静态方法发送，需每次都将smtp、账号、收件人、内容提供<br>
 * &nbsp;&nbsp; JMail.sendMail(smtp, fromUserAddress, fromUserPass,recipients,subject,content);
 * @author Jadic
 * @created 2014-5-13
 */
public class JMail {

	private String smtpHost;// smtp服务器地址
	private String userName;// 发送方账号
	private String userPass;// 发送方密码
	private List<String> recipientList;// 收件人列表

	private Message msg;
	private boolean isMailConfigInited;

	private final static String VALID_EMAIL_REGEX = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
	private final static Pattern VALID_EMAIL_PATTERN = Pattern.compile(VALID_EMAIL_REGEX, Pattern.CASE_INSENSITIVE);

	private final static String SEND_MAIL_OK = "OK";
	private final static String SEND_MAIL_NO_RECIPIENTS = "Fail:no valid recipient set";

	private JMail(String smtp, String fromUserAddress, String fromUserPass) {
		this.smtpHost = smtp;
		this.userName = fromUserAddress;
		this.userPass = fromUserPass;

		recipientList = new ArrayList<String>();
		isMailConfigInited = false;
	}

	/**
	 * @param smtp smtp服务地址
	 * @param fromUserAddress  发件人邮箱账号
	 * @param fromUserPass 发件人邮箱密码
	 * @param recipient 收件人地址(列表),支持多个收件人以";"分割
	 */
	public JMail(String smtp, String fromUserAddress, String fromUserPass, String recipient) {
		this(smtp, fromUserAddress, fromUserPass);
		String[] recs = recipient.split(";");
		for (String rec : recs) {
			if (isValidEmail(rec)) {
				this.recipientList.add(rec);
			}
		}
	}

	/**
	 * @param smtp smtp服务地址
	 * @param fromUserAddress 发件人邮箱账号
	 * @param fromUserPass 发件人邮箱密码
	 * @param recipients  收件人地址列表
	 */
	public JMail(String smtp, String fromUserAddress, String fromUserPass, String... recipients) {
		this(smtp, fromUserAddress, fromUserPass);
		for (String recipient : recipients) {
			if (isValidEmail(recipient)) {
				this.recipientList.add(recipient);
			}
		}
	}

	/**
	 * @param smtp smtp服务地址
	 * @param fromUserAddress 发件人邮箱账号
	 * @param fromUserPass 发件人邮箱密码
	 * @param recipientList 收件人地址列表
	 */
	public JMail(String smtp, String fromUserAddress, String fromUserPass, List<String> recipientList) {
		this(smtp, fromUserAddress, fromUserPass);
		for (String recipient : recipientList) {
			if (isValidEmail(recipient)) {
				this.recipientList.add(recipient);
			}
		}
	}

	/**
	 * 发送邮件,主题默认"default"
	 * @param content  发送内容
	 * @return "ok" 成功发送，否则返回失败提示
	 */
	public String sendMail(String content) {
		return sendMail("default", content);
	}

	/**
	 * @param subject	邮件主题
	 * @param content	邮件内容
	 * @return "ok" 成功发送，否则返回失败提示
	 */
	public String sendMail(String subject, String content) {
		if (this.recipientList.size() <= 0) {
			return SEND_MAIL_NO_RECIPIENTS;
		}

		if (!isMailConfigInited) {
			try {
				initMailConfig();
			} catch (AddressException e) {
				return "fail: address err[" + e.getMessage().trim() + "]";
			} catch (MessagingException e) {
				return "fail: MessagingErr[" + e.getMessage().trim() + "]";
			}
		}
		try {
			msg.setSentDate(new Date());
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		try {
			msg.setSubject(MimeUtility.encodeText(subject, "gbk", "B"));

			MimeMultipart mmp = new MimeMultipart();
			MimeBodyPart mbp = new MimeBodyPart();
			mbp.setContent(content, "text/plain;charset=gbk");
			mmp.addBodyPart(mbp);
			msg.setContent(mmp);
			Transport.send(msg);
		} catch (UnsupportedEncodingException e) {
			System.out.println(e.getMessage().trim());
			return "fail: unsupported encoding[" + e.getMessage().trim() + "]";
		} catch (MessagingException e) {
			System.out.println(e.getMessage().trim());
			return "fail: MessagingErr[" + e.getMessage().trim() + "]";
		}

		return SEND_MAIL_OK;
	}

	private void initMailConfig() throws AddressException, MessagingException {
		Properties props = new Properties();
		// 发送服务器需要身份验证
		props.setProperty("mail.smtp.auth", "true");
		// 设置邮件服务器主机名
		props.setProperty("mail.host", smtpHost);
		// 发送邮件协议名称
		props.setProperty("mail.transport.protocol", "smtp");

		Session session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, userPass);
			}
		});
		msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(userName));
		msg.addRecipients(Message.RecipientType.TO, getRecipients());
		isMailConfigInited = true;
	}

	/**
	 * 
	 * @param smtp	smtp地址
	 * @param fromUserAddress 发件人账号
	 * @param fromUserPass	发件人账号密码
	 * @param recipients 收件人(列表),多个收件人以";"分割
	 * @param subject 邮件主题
	 * @param content 邮件内容
	 * @return "ok" 成功发送，否则返回失败提示
	 */
	public static String sendMail(String smtp, String fromUserAddress, String fromUserPass, String recipients, String subject,
			String content) {
		final String userName = fromUserAddress;
		final String userPass = fromUserPass;
		Properties props = new Properties();
		// 发送服务器需要身份验证
		props.setProperty("mail.smtp.auth", "true");
		// 设置邮件服务器主机名
		props.setProperty("mail.host", smtp);
		// 发送邮件协议名称
		props.setProperty("mail.transport.protocol", "smtp");

		Session session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, userPass);
			}
		});
		Message msg = new MimeMessage(session);
		try {
			msg.setFrom(new InternetAddress(userName));
			String[] recipientAry = recipients.split(";");
			InternetAddress[] recipientsAddress = new InternetAddress[recipientAry.length];
			for (int i = 0; i < recipientAry.length; i++) {
				if (isValidEmail(recipientAry[i])) {
					recipientsAddress[i] = new InternetAddress(recipientAry[i]);
				}
			}
			msg.addRecipients(Message.RecipientType.TO, recipientsAddress);
			msg.setSentDate(new Date());
			msg.setSubject(MimeUtility.encodeText(subject, "gbk", "B"));

			MimeMultipart mmp = new MimeMultipart();
			MimeBodyPart mbp = new MimeBodyPart();
			mbp.setContent(content, "text/plain;charset=gbk");
			mmp.addBodyPart(mbp);
			msg.setContent(mmp);
			Transport.send(msg);
		} catch (UnsupportedEncodingException e) {
			return "fail: unsupported encoding[" + e.getMessage().trim() + "]";
		} catch (MessagingException e) {
			return "fail: MessagingErr[" + e.getMessage().trim() + "]";
		}

		return SEND_MAIL_OK;
	}

	private InternetAddress[] getRecipients() throws AddressException {
		if (recipientList.size() > 0) {
			InternetAddress[] recipients = new InternetAddress[recipientList.size()];
			for (int i = 0; i < recipients.length; i++) {
				recipients[i] = new InternetAddress(this.recipientList.get(i));
			}
			return recipients;
		}
		return new InternetAddress[0];
	}

	private static boolean isValidEmail(String email) {
		return email != null && VALID_EMAIL_PATTERN.matcher(email).find();
	}
	
	public static void main(String args[]){
		Scanner sc = new Scanner(System.in);
		System.out.print("收件人邮箱地址：");
		String email = sc.nextLine();
		JMail mail = new JMail("smtp.163.com", "lzs716@163.com", "********", email);
		System.out.print("主题：");
		String title = sc.nextLine();
		System.out.print("内容：");
		String content = sc.nextLine();
		mail.sendMail(title, content);
	}
}
