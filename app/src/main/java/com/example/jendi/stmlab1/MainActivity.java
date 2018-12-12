package com.example.jendi.stmlab1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    boolean isPlayerServer;
    String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonServer = findViewById(R.id.buttonServer);
        Button buttonClient = findViewById(R.id.buttonClient);
        EditText editText = findViewById(R.id.editText);
        editText.setText("172.20.10.6");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        buttonServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = findViewById(R.id.editText);
                ip = editText.getText().toString();
                isPlayerServer = true;
                ConstraintLayout layout = findViewById(R.id.layout);
                DrawView drawView = null;
                try {
                    drawView = new DrawView(getApplicationContext(), isPlayerServer, ip);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                layout.addView(drawView);
                view.setVisibility(View.INVISIBLE);
                MainActivity.this.findViewById(R.id.buttonClient).setVisibility(View.INVISIBLE);
            }
        });
        buttonClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = findViewById(R.id.editText);
                ip = editText.getText().toString();
                isPlayerServer = false;
                ConstraintLayout layout = findViewById(R.id.layout);
                DrawView drawView = null;
                try {
                    drawView = new DrawView(getApplicationContext(), isPlayerServer, ip);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                layout.addView(drawView);
                view.setVisibility(View.INVISIBLE);
                MainActivity.this.findViewById(R.id.buttonServer).setVisibility(View.INVISIBLE);
            }
        });
    }
}

class DrawView extends View {
    int width, height, xCenter, yCenter, playerWidth, playerHeight, ballSize, screenXCenter, screenYCenter;
    Paint backgroundPaint, elementPaint, textPaint;
    Player clientPlayer, serverPlayer;
    Client client;
    Server server;
    boolean isPlayerServer;
    Ball ball;
    boolean isSetup = true;
    HashMap<Integer, Player> map = new HashMap<>();
    SparseArray<Integer> activePointers;
    private int screenWidth;
    private int screenHeight;

    public DrawView(Context context, boolean isServer, String ip) throws UnknownHostException {
        super(context);
        backgroundPaint = new Paint();
        backgroundPaint.setARGB(255, 0 ,0 ,0);
        elementPaint = new Paint();
        elementPaint.setARGB(255 ,255,255, 255);
        textPaint = new Paint();
        textPaint.setARGB(255, 255, 255 ,255);
        textPaint.setTextSize(48);
        activePointers = new SparseArray<>();
        isPlayerServer = isServer;
        if (isServer) {
            server = new Server();
            server.start();
        }
        else {
            client = new Client(ip);
            client.start();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                if ((event.getX() - 100) > 0 && (event.getX() + 100 < width)) {
                    if (isPlayerServer) {
                        serverPlayer.setPosX((int)event.getX());
                        ServerState serverState = new ServerState(serverPlayer.getPosX(),
                                clientPlayer.getPosX(), ball.getPosX(), ball.getPosY(),
                                serverPlayer.getScore(), clientPlayer.getScore());
                        server.setServerState(serverState);
                    }
                    else {
                        clientPlayer.setPosX((int)event.getX());
                        ClientState clientState = new ClientState(clientPlayer.getPosX());
                        client.setClientState(clientState);
                    }
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isSetup) {
            screenWidth = getWidth();
            screenHeight = getHeight();
            screenXCenter = getWidth()/2;
            screenYCenter = getHeight()/2;
            width = 1080;
            height = 1620;
            ballSize = width/100;
            playerWidth = width/10;
            playerHeight = height/10;
            xCenter = width/2;
            yCenter = height/2;
            clientPlayer = new Player(xCenter, height/10, width, height);
            serverPlayer = new Player(xCenter, height - height/10, width, height);
            ball = new Ball(xCenter, yCenter, ballSize);
            if (isPlayerServer) {
                server.setServerState(new ServerState(serverPlayer.getPosX(), clientPlayer.getPosX(),
                        ball.getPosX(),ball.getPosY(), serverPlayer.getScore(), clientPlayer.getScore()));
            }
            else {
                client.setClientState(new ClientState(clientPlayer.getPosX()));
            }
            isSetup = false;
        }
        canvas.translate((screenWidth-width)/2, (screenHeight-height)/2);

        //Tlo
        canvas.drawRect(0 ,0, width, height, backgroundPaint);

        //Paletki
        canvas.drawRect(clientPlayer.getRect(), elementPaint);
        canvas.drawRect(serverPlayer.getRect(), elementPaint);

        //Pilka
        canvas.drawRect(ball.getRect(), elementPaint);

        //Wynik
        canvas.save();
        canvas.rotate(-90, 10, height/2);
        canvas.drawText(String.valueOf(serverPlayer.getScore())+"    SCORE    " +
                String.valueOf(clientPlayer.getScore()),-50, (height/2)+40, textPaint);
        canvas.restore();
        //Tutaj odswieza sie ekran
        invalidate();
    }

    @Override
    public void invalidate() {
        if (isPlayerServer) {
            ClientState clientState = server.getClientState();
            if (clientState != null) {
                clientPlayer.setPosX(clientState.getX());
            }
            //Kolizja z ekranem
            int newPosX = ball.getPosX() + ball.getSpeed()*ball.getDirX();
            int newPosY = ball.getPosY() + ball.getSpeed()*ball.getDirY();

            //Obsluga X
            if (newPosX > width || newPosX < 0) {
                ball.setDirX(-1*ball.getDirX());
            }
            ball.setPosX(newPosX);

            //Obsluga Y
            if (ball.getRect().intersect(clientPlayer.getRect()) || ball.getRect().intersect(serverPlayer.getRect())) {
                ball.setSpeed(ball.getSpeed() + 1);
                ball.rebound();
                ball.setPosY(ball.getPosY() + ball.getSpeed()*ball.getDirY());
            }
            else if (newPosY > height) {
                //Wygrywa clientPlayer
                clientPlayer.setScore(clientPlayer.getScore()+1);
                ball.resetBall(width, height);
            }
            else if (newPosY < 0) {
                serverPlayer.setScore(serverPlayer.getScore()+1);
                ball.resetBall(width, height);
            }
            else ball.setPosY(newPosY);
            ServerState serverState = new ServerState(serverPlayer.getPosX(), clientPlayer.getPosX(),
                    ball.getPosX(), ball.getPosY(), serverPlayer.getScore(), clientPlayer.getScore());
            server.setServerState(serverState);
        }
        else { //jestem klientem
            ServerState serverState = client.getServerState();
            if (serverState != null) {
                serverPlayer.setPosX(serverState.getServerX());
                clientPlayer.setPosX(serverState.getClientX());
                ball.setPosX(serverState.getBallX());
                ball.setPosY(serverState.getBallY());
                serverPlayer.setScore(serverState.getServerScore());
                clientPlayer.setScore(serverState.getClientScore());
            }
        }
        super.invalidate();
    }
}

class Player {
    private Rect rect;
    private int posX;
    private int posY;
    private int score = 0;
    private int width;
    private int height;

    Player(int x, int y, int w, int h) {
        posX = x;
        posY = y;
        this.width = w;
        this.height = h;
        rect = new Rect(posX - w/10, posY - h/150, posX + w/10, posY + h/150);
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
        refreshRect();
    }

    private void refreshRect() {
        this.rect = new Rect(posX - width/10, posY - height/150, posX + width/10, posY + height/150);
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
        refreshRect();
    }

    public Rect getRect() {
        return rect;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}

class Ball {
    Rect rect;
    private int posX;
    private int posY;
    private int dirX = 1;
    private int dirY = 1;
    private int speed = 2;
    private int size;

    Ball(int x, int y, int size) {
        posX = x;
        posY = y;
        this.size = size;
        rect = new Rect(posX-size, posY-size,posX+size, posY+size);
    }

    public void rebound() {
        this.dirY*=-1;
    }

    public void refreshRect() {
        this.rect = new Rect(posX-size, posY-size,posX+size, posY+size);
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
        refreshRect();
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
        refreshRect();
    }

    public Rect getRect() {
        return rect;
    }

    public int getDirX() {
        return dirX;
    }

    public void setDirX(int dirX) {
        this.dirX = dirX;
    }

    public int getDirY() {
        return dirY;
    }

    public void setDirY(int dirY) {
        this.dirY = dirY;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void resetBall(int x, int y) {
        this.posX = x/2;
        this.posY = y/2;
        this.dirX = 1;
        this.dirY = 1;
        this.speed = 2;
        refreshRect();
    }
}

//klient potrzebuje IP
class Client extends Thread {
    private Socket socket;
    private InetAddress ip;
    private ServerState serverState;
    private ClientState clientState;
    private static final int PORT = 8080;

    Client(String ip) throws UnknownHostException {
        this.ip = InetAddress.getByName(ip);
    }

    @Override
    public void run() {
        try {
            socket = new Socket(ip, PORT);
            while (true) {
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                serverState = (ServerState) inputStream.readObject();
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(clientState);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ServerState getServerState() {
        return serverState;
    }

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
    }

    public ClientState getClientState() {
        return clientState;
    }

    public void setClientState(ClientState clientState) {
        this.clientState = clientState;
    }
}

class Server extends Thread {
    private ServerSocket serverSocket;
    private ServerState serverState;
    private ClientState clientState;
    static final int PORT = 8080;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            Socket socket = serverSocket.accept();
            while (true) {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(serverState);
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                clientState = (ClientState) inputStream.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ServerState getServerState() {
        return serverState;
    }

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
    }

    public ClientState getClientState() {
        return clientState;
    }

    public void setClientState(ClientState clientState) {
        this.clientState = clientState;
    }
}

class ServerState implements Serializable {
    private Integer serverX, clientX, ballX, ballY, serverScore, clientScore;

    public ServerState(Integer serverX, Integer clientX, Integer ballX, Integer ballY,
                       Integer serverScore, Integer clientScore) {
        this.serverX = serverX;
        this.clientX = clientX;
        this.ballX = ballX;
        this.ballY = ballY;
        this.serverScore = serverScore;
        this.clientScore = clientScore;
    }

    public Integer getServerX() {
        return serverX;
    }

    public void setServerX(Integer serverX) {
        this.serverX = serverX;
    }

    public Integer getClientX() {
        return clientX;
    }

    public void setClientX(Integer clientX) {
        this.clientX = clientX;
    }

    public Integer getBallX() {
        return ballX;
    }

    public void setBallX(Integer ballX) {
        this.ballX = ballX;
    }

    public Integer getBallY() {
        return ballY;
    }

    public void setBallY(Integer ballY) {
        this.ballY = ballY;
    }

    public Integer getServerScore() {
        return serverScore;
    }

    public void setServerScore(Integer serverScore) {
        this.serverScore = serverScore;
    }

    public Integer getClientScore() {
        return clientScore;
    }

    public void setClientScore(Integer clientScore) {
        this.clientScore = clientScore;
    }
}

class ClientState implements Serializable {
    private Integer x;

    public ClientState(Integer x) {
        this.x = x;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }
}

//class ClientSender extends Thread {
//    Socket socket;
//    int posX;
//    static final int PORT = 3181;
//    String ip;
//
//    ClientSender(String ip) {
//        this.ip = ip;
//    }
//
//    public void setPosX(int x) {
//        this.posX = x;
//    }
//
//    @Override
//    public void run() {
//        try {
//            socket = new Socket(ip, PORT);
//            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
//            outputStream.writeObject(posX);
//            outputStream.flush();
//            outputStream.close();
//            socket.close();
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//    //co wysyla klient?
//}
//
//class ClientReceiver extends Thread {
//    ServerSocket serverSocket;
//    Socket socket;
//    static final int PORT = 3180;
//    Integer serverPosX, myPosX, ballX, ballY, serverSc, mySc;
//
//    @Override
//    public void run() {
//        try {
//            serverSocket = new ServerSocket();
//            serverSocket.setReuseAddress(true);
//            serverSocket.bind(new InetSocketAddress(PORT));
//            socket = serverSocket.accept();
//            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
//            serverPosX = (int) inputStream.readObject();
//            myPosX = (int) inputStream.readObject();
//            ballX = (int) inputStream.readObject();
//            ballY = (int) inputStream.readObject();
//            serverSc = (int) inputStream.readObject();
//            mySc = (int) inputStream.readObject();
//        }
//        catch (Exception e) { e.printStackTrace(); }
//    }
//
//    public Integer getServerPosX() {
//        return serverPosX;
//    }
//
//    public Integer getMyPosX() {
//        return myPosX;
//    }
//
//    public Integer getBallX() {
//        return ballX;
//    }
//
//    public Integer getBallY() {
//        return ballY;
//    }
//
//    public Integer getServerSc() {
//        return serverSc;
//    }
//
//    public Integer getMySc() {
//        return mySc;
//    }
//}
//
//class ServerReceiver extends Thread {
//    ServerSocket serverSocket;
//    Socket clientSocket;
//    static final int PORT = 3181;
//    Integer clientX;
//
//    @Override
//    public void run() {
//        try {
//            serverSocket = new ServerSocket();
//            serverSocket.setReuseAddress(true);
//            serverSocket.bind(new InetSocketAddress(PORT));
//            clientSocket = serverSocket.accept();
//            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
//            clientX = (int) inputStream.readObject();
//            inputStream.close();
//            clientSocket.close();
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//}
//
//class ServerSender extends Thread {
//    Socket socket;
//    Integer myPosX, enemyPosX, ballX, ballY, serverScore, clientScore;
//    static final int PORT = 3180;
//    String ip;
//
//    public ServerSender(Integer myX, Integer enemyX, Integer bX, Integer bY,
//                             Integer serverSc, Integer clientSc, String ip) {
//        this.myPosX = myX;
//        this.enemyPosX = enemyX;
//        this.ballX = bX;
//        this.ballY = bY;
//        this.serverScore = serverSc;
//        this.clientScore = clientSc;
//        this.ip = ip;
//    }
//
//    @Override
//    public void run() {
//        try {
//            socket = new Socket(ip, PORT);
//            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
//            outputStream.writeObject(myPosX);
//            outputStream.writeObject(enemyPosX);
//            outputStream.writeObject(ballX);
//            outputStream.writeObject(ballY);
//            outputStream.writeObject(serverScore);
//            outputStream.writeObject(clientScore);
//            outputStream.flush();
//            outputStream.close();
//            socket.close();
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//}
