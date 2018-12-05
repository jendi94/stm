package com.example.jendi.stmlab1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConstraintLayout layout = findViewById(R.id.layout);
        DrawView drawView = new DrawView(this);
        layout.addView(drawView);
    }
}

class DrawView extends View {
    int width, height, xCenter, yCenter;
    Paint backgroundPaint, elementPaint, textPaint;
    Player playerOne, playerTwo;
    boolean playerOneTouch, playerTwoTouch;
    Ball ball;
    boolean isSetup = true;
    HashMap<Integer, Player> map = new HashMap<>();
    SparseArray<Integer> activePointers;

    public DrawView(Context context) {
        super(context);
        backgroundPaint = new Paint();
        backgroundPaint.setARGB(255, 0 ,0 ,0);
        elementPaint = new Paint();
        elementPaint.setARGB(255 ,255,255, 255);
        textPaint = new Paint();
        textPaint.setARGB(255, 255, 255 ,255);
        textPaint.setTextSize(48);
        playerOneTouch = false;
        playerTwoTouch = false;
        activePointers = new SparseArray<>();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                int touchX = (int) event.getX(pointerIndex);
                int touchY = (int) event.getY(pointerIndex);

                if (isPlayerTouched(playerOne, touchX, touchY)) {
                    map.put(pointerId, playerOne);
                }
                if (isPlayerTouched(playerTwo, touchX ,touchY )) {
                    map.put(pointerId, playerTwo);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                Iterator it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    Player temp = map.get(pair.getKey());
                    int q = 17;
                    if ((int) event.getX((Integer) pair.getKey()) - 100 > 0 &&
                        (int) event.getX((Integer) pair.getKey()) + 100 < width) {
                        map.get(pair.getKey()).setPosX((int)event.getX((Integer) pair.getKey()));
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                map.remove(pointerId);
                break;


        }
        return true;
    }

    private boolean isPlayerTouched(Player player, int x, int y) {
        int left = player.getRect().left;
        int right = player.getRect().right;
        int top = player.getRect().top;
        int bottom = player.getRect().bottom;

        if (left < x && right > x && y > (top-30) && y < (bottom+30)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isSetup) {
            width = getWidth();
            height = getHeight();
            xCenter = width/2;
            yCenter = height/2;
            playerOne = new Player(xCenter, 100);
            playerTwo = new Player(xCenter, height - 100);
            ball = new Ball(xCenter, yCenter);
            isSetup = false;
        }

        //Tlo
        canvas.drawRect(0 ,0, width, height, backgroundPaint);

        //Paletki
        canvas.drawRect(playerOne.getRect(), elementPaint);
        canvas.drawRect(playerTwo.getRect(), elementPaint);

        //Pilka
        canvas.drawRect(ball.getRect(), elementPaint);

        //Wynik
        canvas.save();
        canvas.rotate(-90, 10, height/2);
        canvas.drawText(String.valueOf(playerTwo.getScore())+"    SCORE    " +
                String.valueOf(playerOne.getScore()),-50, (height/2)+40, textPaint);
        canvas.restore();

        //Tutaj odswieza sie ekran
        invalidate();
    }

    @Override
    public void invalidate() {
        //Kolizja z ekranem
        int newPosX = ball.getPosX() + ball.getSpeed()*ball.getDirX();
        int newPosY = ball.getPosY() + ball.getSpeed()*ball.getDirY();

        //Obsluga X
        if (newPosX > width || newPosX < 0) {
            ball.setDirX(-1*ball.getDirX());
        }
        ball.setPosX(newPosX);

        //Obsluga Y
        if (ball.getRect().intersect(playerOne.getRect()) || ball.getRect().intersect(playerTwo.getRect())) {
            ball.setSpeed(ball.getSpeed() + 3);
            ball.rebound();
            ball.setPosY(ball.getPosY() + ball.getSpeed()*ball.getDirY());
        }
        else if (newPosY > height) {
            //Wygrywa playerOne
            playerOne.setScore(playerOne.getScore()+1);
            ball.resetBall(width, height);
        }
        else if (newPosY < 0) {
            playerTwo.setScore(playerTwo.getScore()+1);
            ball.resetBall(width, height);
        }
        else ball.setPosY(newPosY);
        super.invalidate();
    }
}

class Player {
    private Rect rect;
    private int posX;
    private int posY;
    private int score = 0;

    Player(int x, int y) {
        posX = x;
        posY = y;
        rect = new Rect(posX - 100, posY - 10, posX + 100, posY + 10);
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
        refreshRect();
    }

    private void refreshRect() {
        this.rect = new Rect(posX - 100, posY - 10, posX + 100, posY + 10);
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
    private int speed = 5;

    Ball(int x, int y) {
        posX = x;
        posY = y;
        rect = new Rect(posX-10, posY-10,posX+10, posY+10);
    }

    public void rebound() {
        this.dirY*=-1;
    }

    public void refreshRect() {
        this.rect = new Rect(posX-10, posY-10,posX+10, posY+10);
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
        this.speed = 5;
        refreshRect();
    }
}
