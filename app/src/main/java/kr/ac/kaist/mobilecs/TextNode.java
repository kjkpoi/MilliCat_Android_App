package kr.ac.kaist.mobilecs;


public class TextNode {
    private float mPosX;
    private float mPosY;
    private String mText;

    public TextNode(float x, float y, String text) {
        mPosX = x;
        mPosY = y;
        mText = text;
    }

    public float getX() {
        return mPosX;
    }

    public float getY() {
        return mPosY;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }
}
