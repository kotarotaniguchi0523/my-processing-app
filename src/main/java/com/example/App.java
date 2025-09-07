package com.example;

import processing.core.PApplet;

public class App extends PApplet {

    public static void main(String[] args) {
        PApplet.main("com.example.App");
    }

    @Override
    public void settings() {
        size(800, 600);
    }

    @Override
    public void setup() {
        background(240);
        textAlign(CENTER, CENTER);
        textSize(24);
    }

    @Override
    public void draw() {
        background(240);

        // 上半分：描画エリア
        fill(255);
        rect(0, 0, width, height / 2);

        // 下半分：チャットエリア（仮）
        fill(200);
        rect(0, height / 2, width, height / 2);

        // テキスト表示
        fill(0);
        text("描画エリア", width / 2, height / 4);
        text("チャットエリア（ここに後でUI追加）", width / 2, height * 3 / 4);

        // マウス追従の円
        fill(255, 0, 0, 100);
        ellipse(mouseX, mouseY, 50, 50);
    }
}
