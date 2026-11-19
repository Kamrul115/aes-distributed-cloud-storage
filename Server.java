package com.mycompany.cloudsystem;


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private ServerSocket ss;
    private ArrayList<Ch> cl;
    private HashMap<String, Fi> fs;
    private JTextArea la;
    private JFrame fr;

    public Server(int p) {
        cl = new ArrayList<>();
        fs = new HashMap<>();
        ui();

        lg("Server starting...");

        new Thread(() -> {
            try {
                ss = new ServerSocket(p);
                lg("Server started on port " + p);

                while (true) {
                    Socket sk = ss.accept();
                    Ch ch = new Ch(sk);
                    cl.add(ch);
                    new Thread(ch).start();
                    lg("New client connected: " + sk.getInetAddress());
                }
            } catch (IOException e) {
                lg("Srv err: " + e.getMessage());
            }
        }).start();
    }

    private void ui() {
        fr = new JFrame("Server Logs");
        fr.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        fr.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                sd();
            }
        });

        fr.setSize(600, 400);
        fr.setLayout(new BorderLayout(10, 10));

        JPanel tp = new JPanel();
        JLabel tl = new JLabel("Srv Logs");
        tl.setFont(new Font("Arial", Font.BOLD, 18));
        tp.add(tl);
        fr.add(tp, BorderLayout.NORTH);

        la = new JTextArea();
        la.setEditable(false);
        la.setFont(new Font("Monospace", Font.PLAIN, 12));
        la.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane sp = new JScrollPane(la);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        fr.add(sp, BorderLayout.CENTER);

        fr.setLocationRelativeTo(null);
        fr.setVisible(true);
    }

    private void lg(String m) {
        SwingUtilities.invokeLater(() -> {
            String ts = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
            la.append("[" + ts + "] " + m + "\n");
            la.setCaretPosition(la.getDocument().getLength());
        });
    }

    private void bf() {
        if (cl != null) {
            for (Ch c : cl) {
                c.sf();
            }
        } else {
            lg("Error: Cl list not initialized");
        }
    }

    private void sd() {
        int c = JOptionPane.showConfirmDialog(fr,
                "Confirm server shutdown?",
                "Shutdown",
                JOptionPane.YES_NO_OPTION);

        if (c == JOptionPane.YES_OPTION) {
            lg("Server shutting down...");
            try {
                for (Ch cl : cl) {
                    cl.cln();
                }
                if (ss != null && !ss.isClosed()) {
                    ss.close();
                }
                lg("Server shutdown complete.");
            } catch (IOException e) {
                lg("Error during shutdown: " + e.getMessage());
            } finally {
                System.exit(0);
            }
        }
    }

    private class Ch implements Runnable {
        private Socket sk;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String cn;

        public Ch(Socket sk) {
            this.sk = sk;
            try {
                out = new ObjectOutputStream(sk.getOutputStream());
                in = new ObjectInputStream(sk.getInputStream());
                this.cn = "Cl-" + sk.getInetAddress().getHostAddress();
                lg("Streams set up for " + cn);
            } catch (IOException e) {
                lg("Error creating streams for " + cn + ": " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String cmd = (String) in.readObject();

                    switch (cmd) {
                        case "UL":
                            ul();
                            break;
                        case "DL":
                            dl();
                            break;
                        case "LS":
                            sf();
                            break;
                        default:
                            lg("Unknown cmd from " + cn + ": " + cmd);
                    }
                }
            } catch (EOFException e) {
                lg(cn + " disconnected");
            } catch (Exception e) {
                lg("Error handling client " + cn + ": " + e.getMessage());
            } finally {
                cln();
            }
        }

        private void ul() {
            try {
                String fn = (String) in.readObject();
                byte[] ed = (byte[]) in.readObject();

                Fi fi = new Fi(fn, ed, cn);
                fs.put(fn, fi);

                lg(cn + " uploaded file: " + fn + " (Size: " + (ed.length / 1024) + "KB)");
                bf();
            } catch (Exception e) {
                lg("Error during uploaded from " + cn + ": " + e.getMessage());
            }
        }

        private void dl() {
            try {
                String fn = (String) in.readObject();
                Fi fi = fs.get(fn);

                if (fi != null) {
                    out.writeObject(fi);
                    lg(cn + " downloaded file: " + fn);
                } else {
                    out.writeObject(null);
                    lg(cn + " tried downloading non-existent file: " + fn);
                }
            } catch (Exception e) {
                lg("Error during download for " + cn + ": " + e.getMessage());
            }
        }

        private void sf() {
            try {
                ArrayList<Fi> fl = new ArrayList<>(fs.values());
                out.writeObject(fl);
                out.flush();
                lg("Sent file list to " + cn + " (" + fl.size() + " files)");
            } catch (IOException e) {
                lg("Error sending file list to " + cn + ": " + e.getMessage());
            }
        }

        private void cln() {
            cl.remove(this);
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (sk != null && !sk.isClosed()) sk.close();
            } catch (IOException e) {
                lg("Error cleaning up " + cn + ": " + e.getMessage());
            }
            lg(cn + " cleaned up");
            bf();
        }
    }

    private static class Fi implements Serializable {
        String fn;
        byte[] ed;
        String own;

        public Fi(String fn, byte[] ed, String own) {
            this.fn = fn;
            this.ed = ed;
            this.own = own;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Server(5050);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Server failed: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
