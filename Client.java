package com.mycompany.cloudsystem;


import javax.crypto.*;
import javax.crypto.spec.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.*;
import java.util.*;
import java.util.Base64;
import javax.swing.border.EmptyBorder;

public class Client extends JFrame {
    private Socket s;
    private ObjectOutputStream o;
    private ObjectInputStream i;
    private JList<FileInfo> fl;
    private DefaultListModel<FileInfo> lm;
    private Map<String, String> fk;

    public Client(String h, int p) {
        super("Cloud Client");
        fk = new HashMap<>();
        ui();
        conn(h, p);
    }

    private void ui() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        lm = new DefaultListModel<>();
        fl = new JList<>(lm);
        fl.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int idx, boolean sel, boolean focus) {
                FileInfo fi = (FileInfo) v;
                return super.getListCellRendererComponent(l, String.format("%s (By: %s)", fi.getFilename(), fi.getUploadedBy()), idx, sel, focus);
            }
        });
        fl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(fl), BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton up = new JButton("Upload");
        up.addActionListener(e -> upload());

        JButton down = new JButton("Download");
        down.addActionListener(e -> download());

        bp.add(up);
        bp.add(down);
        add(bp, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void conn(String h, int p) {
        try {
            s = new Socket(h, p);
            o = new ObjectOutputStream(s.getOutputStream());
            i = new ObjectInputStream(s.getInputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        o.writeObject("LIST");
                        ArrayList<FileInfo> files = (ArrayList<FileInfo>) i.readObject();
                        update(files);
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    err("Lost connection");
                }
            }).start();

        } catch (IOException e) {
            err("Could not connect");
        }
    }

    private byte[] enc(byte[] d, String k) throws Exception {
        SecretKey sk = key(k);
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, sk);
        return c.doFinal(d);
    }

    private byte[] dec(byte[] d, String k) throws Exception {
        SecretKey sk = key(k);
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, sk);
        return c.doFinal(d);
    }

    private SecretKey key(String pw) throws Exception {
        byte[] k = pw.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        k = md.digest(k);
        k = Arrays.copyOf(k, 16);
        return new SecretKeySpec(k, "AES");
    }

    private void upload() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();

            String k = JOptionPane.showInputDialog(this, "Enter encryption key:", "Key", JOptionPane.QUESTION_MESSAGE);

            if (k == null || k.trim().isEmpty()) {
                err("Key required");
                return;
            }

            try {
                byte[] fd = Files.readAllBytes(f.toPath());
                byte[] ed = enc(fd, k);
                fk.put(f.getName(), k);

                o.writeObject("UPLOAD");
                o.writeObject(f.getName());
                o.writeObject(ed);

                JOptionPane.showMessageDialog(this, "Uploaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                err("Upload error: " + e.getMessage());
            }
        }
    }

    private void download() {
        FileInfo sf = fl.getSelectedValue();
        if (sf == null) {
            err("Select a file");
            return;
        }

        String k = JOptionPane.showInputDialog(this, "Enter decryption key:", "Key", JOptionPane.QUESTION_MESSAGE);

        if (k == null || k.trim().isEmpty()) {
            err("Key required");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(sf.getFilename()));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                o.writeObject("DOWNLOAD");
                o.writeObject(sf.getFilename());

                FileInfo fi = (FileInfo) i.readObject();
                byte[] dd = dec(fi.getEncryptedData(), k);

                File f = fc.getSelectedFile();
                Files.write(f.toPath(), dd);

                JOptionPane.showMessageDialog(this, "Downloaded and decrypted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                err("Download error");
            }
        }
    }

    private void update(ArrayList<FileInfo> files) {
        SwingUtilities.invokeLater(() -> {
            lm.clear();
            for (FileInfo f : files) {
                lm.addElement(f);
            }
        });
    }

    private void err(String m) {
        JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client("localhost", 5050));
    }
}
