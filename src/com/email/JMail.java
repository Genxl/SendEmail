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
 * �����ʼ�����<br>
 * ���ṩ�ʼ������ṩ�̵�smtp��ַ�������˺š������ռ�������<br><br>
 * ʹ�÷�ʽ:<br>
 * 1.ʵ����JMail�����ʺ϶��������ͬ�˺���Ϣ���͵���ͬ�ռ���<br>
 * &nbsp;&nbsp; JMail jMail = new JMail(...);<br>
 * &nbsp;&nbsp; jMail.sendMail(subject, content);<br>
 * 2.ֱ�ӵ���JMail��̬�������ͣ���ÿ�ζ���smtp���˺š��ռ��ˡ������ṩ<br>
 * &nbsp;&nbsp; JMail.sendMail(smtp, fromUserAddress, fromUserPass,recipients,subject,content);
 * @author Jadic
 * @created 2014-5-13
 */
public class JMail {

	private String smtpHost;// smtp��������ַ
	private String userName;// ���ͷ��˺�
	private String userPass;// ���ͷ�����
	private List<String> recipientList;// �ռ����б�

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
	 * @param smtp smtp�����ַ
	 * @param fromUserAddress  �����������˺�
	 * @param fromUserPass ��������������
	 * @param recipient �ռ��˵�ַ(�б�),֧�ֶ���ռ�����";"�ָ�
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
	 * @param smtp smtp�����ַ
	 * @param fromUserAddress �����������˺�
	 * @param fromUserPass ��������������
	 * @param recipients  �ռ��˵�ַ�б�
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
	 * @param smtp smtp�����ַ
	 * @param fromUserAddress �����������˺�
	 * @param fromUserPass ��������������
	 * @param recipientList �ռ��˵�ַ�б�
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
	 * �����ʼ�,����Ĭ��"default"
	 * @param content  ��������
	 * @return "ok" �ɹ����ͣ����򷵻�ʧ����ʾ
	 */
	public String sendMail(String content) {
		return sendMail("default", content);
	}

	/**
	 * @param subject	�ʼ�����
	 * @param content	�ʼ�����
	 * @return "ok" �ɹ����ͣ����򷵻�ʧ����ʾ
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
		// ���ͷ�������Ҫ�����֤
		props.setProperty("mail.smtp.auth", "true");
		// �����ʼ�������������
		props.setProperty("mail.host", smtpHost);
		// �����ʼ�Э������
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
	 * @param smtp	smtp��ַ
	 * @param fromUserAddress �������˺�
	 * @param fromUserPass	�������˺�����
	 * @param recipients �ռ���(�б�),����ռ�����";"�ָ�
	 * @param subject �ʼ�����
	 * @param content �ʼ�����
	 * @return "ok" �ɹ����ͣ����򷵻�ʧ����ʾ
	 */
	public static String sendMail(String smtp, String fromUserAddress, String fromUserPass, String recipients, String subject,
			String content) {
		final String userName = fromUserAddress;
		final String userPass = fromUserPass;
		Properties props = new Properties();
		// ���ͷ�������Ҫ�����֤
		props.setProperty("mail.smtp.auth", "true");
		// �����ʼ�������������
		props.setProperty("mail.host", smtp);
		// �����ʼ�Э������
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
		System.out.print("�ռ��������ַ��");
		String email = sc.nextLine();
		JMail mail = new JMail("smtp.163.com", "lzs716@163.com", "********", email);
		System.out.print("���⣺");
		String title = sc.nextLine();
		System.out.print("���ݣ�");
		String content = sc.nextLine();
		mail.sendMail(title, content);
	}
}
