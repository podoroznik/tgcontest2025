package org.telegram.ui;

public class ProfileButton {
    private final int imageResId;
    private final String title;
    private final Runnable onClickAction;

    public ProfileButton(int imageResId, String title, Runnable onClickAction) {
        this.imageResId = imageResId;
        this.title = title;
        this.onClickAction = onClickAction;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getTitle() {
        return title;
    }

    public void performClick() {
        onClickAction.run();
    }
}