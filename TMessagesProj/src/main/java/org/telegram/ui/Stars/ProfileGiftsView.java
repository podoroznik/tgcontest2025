package org.telegram.ui.Stars;

import static org.telegram.messenger.AndroidUtilities.accelerateInterpolator;
import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.ui.Stars.StarsController.findAttribute;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.ButtonBounce;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.ProfileActivity;

import java.util.ArrayList;
import java.util.HashSet;

public class ProfileGiftsView extends View implements NotificationCenter.NotificationCenterDelegate {

    private final int currentAccount;
    private final long dialogId;
    private final View avatarContainer;
    private final ProfileActivity.AvatarImageView avatarImage;
    private final Theme.ResourcesProvider resourcesProvider;
    private Interpolator accelerateInterpolator1 = new AccelerateInterpolator(0.6f);
    private Interpolator accelerateInterpolator3 = new AccelerateInterpolator(0.8f);
    private Interpolator accelerateInterpolator2 = new AccelerateInterpolator(0.55f);

    private  final Interpolator firstInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            if(input > 0.495) return 1;
            float a = input / 0.495f;
            return accelerateInterpolator2.getInterpolation(a);
        }
    };

    private  final Interpolator secondInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            if(input < 0.33) return 0;
            if(input > 0.66) return 1;
            float a = (input - 0.33f) / 0.33f;
            return accelerateInterpolator1.getInterpolation(a);
        }
    };

    private  final Interpolator thirdInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            if(input < 0.66) return 0;
            float a = (input - 0.66f) / 0.33f;
            return accelerateInterpolator3.getInterpolation(a);
        }
    };


    private final Interpolator scaleInterpolator = new DecelerateInterpolator(1f);

    public ProfileGiftsView(Context context, int currentAccount, long dialogId, @NonNull View avatarContainer, ProfileActivity.AvatarImageView avatarImage, Theme.ResourcesProvider resourcesProvider) {
        super(context);

        this.currentAccount = currentAccount;
        this.dialogId = dialogId;

        this.avatarContainer = avatarContainer;
        this.avatarImage = avatarImage;

        this.resourcesProvider = resourcesProvider;

    }

    private float expandProgress;

    public void setExpandProgress(float progress) {
        if (this.expandProgress != progress) {
            this.expandProgress = progress;
            invalidate();
        }
    }

    private float actionBarProgress;

    public void setActionBarActionMode(float progress) {
//        if (Theme.isCurrentThemeDark()) {
//            return;
//        }
        actionBarProgress = progress;
        invalidate();
    }

    private float vacuumProgress = 0f;

    public void setVacuumProgress(float progress) {
        vacuumProgress = progress;
        invalidate();
    }


    private float left, right, cy;
    private float expandRight, expandY;
    private boolean expandRightPad;
    private final AnimatedFloat expandRightPadAnimated = new AnimatedFloat(this, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
    private final AnimatedFloat rightAnimated = new AnimatedFloat(this, 0, 350, CubicBezierInterpolator.EASE_OUT_QUINT);

    public void setBounds(float left, float right, float cy, boolean animated) {
        boolean changed = Math.abs(left - this.left) > 0.1f || Math.abs(right - this.right) > 0.1f || Math.abs(cy - this.cy) > 0.1f;
        this.left = left;
        this.right = right;
        if (!animated) {
            this.rightAnimated.set(this.right, true);
        }
        this.cy = cy;
        if (changed) {
            invalidate();
        }
    }

    public void setExpandCoords(float right, boolean rightPadded, float y) {
        this.expandRight = right;
        this.expandRightPad = rightPadded;
        this.expandY = y;
        invalidate();
    }

    private float progressToInsets = 1f;

    public void setProgressToStoriesInsets(float progressToInsets) {
        if (this.progressToInsets == progressToInsets) {
            return;
        }
        this.progressToInsets = progressToInsets;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.starUserGiftsLoaded);

        for (Gift gift : gifts) {
            gift.emojiDrawable.addView(this);
        }

        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.starUserGiftsLoaded);

        for (Gift gift : gifts) {
            gift.emojiDrawable.removeView(this);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.starUserGiftsLoaded) {
            if ((long) args[0] == dialogId) {
                update();
            }
        }
    }

    public final class Gift {

        public final long id;
        public final TLRPC.Document document;
        public final long documentId;
        public final int color;
        public final String slug;

        public Gift(TL_stars.TL_starGiftUnique gift) {
            id = gift.id;
            document = gift.getDocument();
            documentId = document == null ? 0 : document.id;
            final TL_stars.starGiftAttributeBackdrop backdrop = findAttribute(gift.attributes, TL_stars.starGiftAttributeBackdrop.class);
            color = backdrop.center_color | 0xFF000000;
            slug = gift.slug;
        }

        public Gift(TLRPC.TL_emojiStatusCollectible status) {
            id = status.collectible_id;
            document = null;
            documentId = status.document_id;
            color = status.center_color | 0xFF000000;
            slug = status.slug;
        }

        public boolean equals(Gift b) {
            return b != null && b.id == id;
        }

        public RadialGradient gradient;
        public final Matrix gradientMatrix = new Matrix();
        public Paint gradientPaint;
        public AnimatedEmojiDrawable emojiDrawable;
        public AnimatedFloat animatedFloat;

        public final RectF bounds = new RectF();
        public final ButtonBounce bounce = new ButtonBounce(ProfileGiftsView.this);

        public void copy(Gift b) {
            gradient = b.gradient;
            emojiDrawable = b.emojiDrawable;
            gradientPaint = b.gradientPaint;
            animatedFloat = b.animatedFloat;
        }

        public void draw(
                Canvas canvas,
                float cx, float cy,
                float ascale, float rotate,
                float alpha,
                float gradientAlpha
        ) {
            if (alpha <= 0.0f) return;
            final float gsz = dp(45);
            bounds.set(cx - gsz / 2, cy - gsz / 2, cx + gsz / 2, cy + gsz / 2);
            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate(rotate);
            final float scale = ascale * bounce.getScale(0.1f);
            canvas.scale(scale, scale);
            if (gradientPaint != null) {
                gradientPaint.setAlpha((int) (0xFF * alpha * gradientAlpha));
                canvas.drawRect(-gsz / 2.0f, -gsz / 2.0f, gsz / 2.0f, gsz / 2.0f, gradientPaint);
            }
            if (emojiDrawable != null) {
                final int sz = dp(24);
                emojiDrawable.setBounds(-sz / 2, -sz / 2, sz / 2, sz / 2);
                emojiDrawable.setAlpha((int) (0xFF * alpha));
                emojiDrawable.draw(canvas);
            }
            canvas.restore();
        }
    }

    private StarsController.GiftsList list;

    public final ArrayList<Gift> oldGifts = new ArrayList<>();
    public final ArrayList<Gift> gifts = new ArrayList<>();
    public final HashSet<Long> giftIds = new HashSet<>();
    public int maxCount;

    public void update() {
        if (!MessagesController.getInstance(currentAccount).enableGiftsInProfile) {
            return;
        }

        maxCount = MessagesController.getInstance(currentAccount).stargiftsPinnedToTopLimit;
        oldGifts.clear();
        oldGifts.addAll(gifts);
        gifts.clear();
        giftIds.clear();

        final TLRPC.EmojiStatus emojiStatus;
        if (dialogId >= 0) {
            final TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            emojiStatus = user == null ? null : user.emoji_status;
        } else {
            final TLRPC.User chat = MessagesController.getInstance(currentAccount).getUser(-dialogId);
            emojiStatus = chat == null ? null : chat.emoji_status;
        }
        if (emojiStatus instanceof TLRPC.TL_emojiStatusCollectible) {
            giftIds.add(((TLRPC.TL_emojiStatusCollectible) emojiStatus).collectible_id);
        }
        list = StarsController.getInstance(currentAccount).getProfileGiftsList(dialogId);
        if (list != null) {
            for (int i = 0; i < list.gifts.size(); i++) {
                final TL_stars.SavedStarGift savedGift = list.gifts.get(i);
                if (!savedGift.unsaved && savedGift.pinned_to_top && savedGift.gift instanceof TL_stars.TL_starGiftUnique) {
                    final Gift gift = new Gift((TL_stars.TL_starGiftUnique) savedGift.gift);
                    if (!giftIds.contains(gift.id)) {
                        gifts.add(gift);
                        giftIds.add(gift.id);
                    }
                }
            }
        }

        boolean changed = false;
        if (gifts.size() != oldGifts.size()) {
            changed = true;
        } else for (int i = 0; i < gifts.size(); i++) {
            if (!gifts.get(i).equals(oldGifts.get(i))) {
                changed = true;
                break;
            }
        }

        for (int i = 0; i < gifts.size(); i++) {
            final Gift g = gifts.get(i);
            Gift oldGift = null;
            for (int j = 0; j < oldGifts.size(); ++j) {
                if (oldGifts.get(j).id == g.id) {
                    oldGift = oldGifts.get(j);
                    break;
                }
            }

            if (oldGift != null) {
                g.copy(oldGift);
            } else {
                g.gradient = new RadialGradient(0, 0, dp(22.5f), new int[]{g.color, Theme.multAlpha(g.color, 0.0f)}, new float[]{0, 1}, Shader.TileMode.CLAMP);
                g.gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                g.gradientPaint.setShader(g.gradient);
                if (g.document != null) {
                    g.emojiDrawable = AnimatedEmojiDrawable.make(currentAccount, AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, g.document);
                } else {
                    g.emojiDrawable = AnimatedEmojiDrawable.make(currentAccount, AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, g.documentId);
                }
                g.animatedFloat = new AnimatedFloat(this, 0, 320, CubicBezierInterpolator.EASE_OUT_QUINT);
                g.animatedFloat.force(0.0f);
                if (isAttachedToWindow()) {
                    g.emojiDrawable.addView(this);
                }
            }
        }

        for (int i = 0; i < oldGifts.size(); i++) {
            final Gift g = oldGifts.get(i);
            Gift newGift = null;
            for (int j = 0; j < gifts.size(); ++j) {
                if (gifts.get(j).id == g.id) {
                    newGift = gifts.get(j);
                    break;
                }
            }
            if (newGift == null) {
                g.emojiDrawable.removeView(this);
                g.emojiDrawable = null;
                g.gradient = null;
            }
        }

        if (changed)
            invalidate();
    }

    private float trueY = 0f;

    public void setTrueY(float y){
        trueY = y;
    }
    public float getTrueY(){
        return trueY;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (gifts.isEmpty() || expandProgress >= 1.0f) return;

        final float ax = avatarContainer.getX();
        final float ay = trueY;
        final float aw = (avatarContainer.getWidth()) * avatarContainer.getScaleX();
        final float ah = (avatarContainer.getHeight()) * avatarContainer.getScaleY();

        canvas.save();
        canvas.clipRect(0, 0, getWidth(), expandY);

        final float acx = ax + aw / 2.0f; // avatar center x
        final float acy = ay + ah / 2.0f; // avatar center y
        final float acy2 = avatarContainer.getY() + ah / 2.0f; // avatar center y2
        final float ar = Math.min(aw, ah) / 2.0f + dp(6); // avatar radius

       final float value1 = thirdInterpolator.getInterpolation(vacuumProgress);
        final float value2 =(secondInterpolator.getInterpolation(vacuumProgress));
        final float value3 =(thirdInterpolator.getInterpolation(vacuumProgress));
        final float value4 =(secondInterpolator.getInterpolation(vacuumProgress));
        final float value5 =(firstInterpolator.getInterpolation(vacuumProgress));
        final float value6 =(firstInterpolator.getInterpolation(vacuumProgress));

        final float scale1 = scaleInterpolator.getInterpolation( Math.max(value1,0.3f));
        final float scale2 = scaleInterpolator.getInterpolation( Math.max(value2,0.3f));
        final float scale3 = scaleInterpolator.getInterpolation( Math.max(value3,0.3f));
        final float scale4 = scaleInterpolator.getInterpolation( Math.max(value4,0.3f));
        final float scale5 = scaleInterpolator.getInterpolation( Math.max(value5,0.3f));
        final float scale6 = scaleInterpolator.getInterpolation( Math.max(value6,0.3f));

        final float closedAlpha = Utilities.clamp01((float) (expandY - (AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight())) / dp(50));


        float startX =  acx;
        float startY =  acy2;

        float midX1 = acx +  (ar + dp(20)) * (float) Math.cos(Math.toRadians(200));
        float midY1 = acy + (ar + dp(20)) * (float) Math.sin(Math.toRadians(200));
        float endX1 = (float) (acx +  (ar + dp(28)) * Math.cos(Math.toRadians(215)));
        float endY1 = (float) (acy +  (ar + dp(28)) * Math.sin(Math.toRadians(215)));
        float currentX1 = (1 - value1) * (1 - value1) * startX
                + 2 * (1 - value1) * value1 * midX1
                + value1 * value1 * endX1;
        float currentY1 = (1 - value1) * (1 - value1) * startY
                + 2 * (1 - value1) * value1 * midY1
                + value1 * value1 * endY1;

        float midX2 = acx +  (ar + dp(40)) * (float) Math.cos(Math.toRadians(-2));
        float midY2 = acy + (ar + dp(40)) * (float) Math.sin(Math.toRadians(-2));
        float endX2 = (float) (acx +  (ar + dp(45)) * Math.cos(Math.toRadians(-25)));
        float endY2 = (float) (acy +  (ar + dp(45)) * Math.sin(Math.toRadians(-25)));
        float currentX2 = (1 - value2) * (1 - value2) * startX
                + 2 * (1 - value2) * value2 * midX2
                + value2 * value2 * endX2;
        float currentY2 = (1 - value2) * (1 - value2) * startY
                + 2 * (1 - value2) * value2 * midY2
                + value2 * value2 * endY2;

        float midX3 = acx +  (ar + dp(15)) * (float) Math.cos(Math.toRadians(28));
        float midY3 = acy + (ar + dp(15)) * (float) Math.sin(Math.toRadians(28));
        float endX3 = (float) (acx +  (ar + dp(27)) * Math.cos(Math.toRadians(22)));
        float endY3 = (float) (acy +  (ar + dp(27)) * Math.sin(Math.toRadians(22)));
        float currentX3 = (1 - value3) * (1 - value3) * startX
                + 2 * (1 - value3) * value3 * midX3
                + value3 * value3 * endX3;
        float currentY3 = (1 - value3) * (1 - value3) * startY
                + 2 * (1 - value3) * value3 * midY3
                + value3 * value3 * endY3;

        float midX4 = acx +  (ar + dp(20)) * (float) Math.cos(Math.toRadians(125));
        float midY4 = acy + (ar + dp(20)) * (float) Math.sin(Math.toRadians(125));
        float endX4 = (float) (acx +  (ar + dp(30)) * Math.cos(Math.toRadians(160)));
        float endY4 = (float) (acy +  (ar + dp(30)) * Math.sin(Math.toRadians(160)));
        float currentX4 = (1 - value4) * (1 - value4) * startX
                + 2 * (1 - value4) * value4 * midX4
                + value4 * value4 * endX4;
        float currentY4 = (1 - value4) * (1 - value4) * startY
                + 2 * (1 - value4) * value4 * midY4
                + value4 * value4 * endY4;

        float midX5 = acx +  (ar + dp(45)) * (float) Math.cos(Math.toRadians(190));
        float midY5 = acy + (ar + dp(45)) * (float) Math.sin(Math.toRadians(190));
        float endX5 = (float) (acx +  (ar + dp(55)) * Math.cos(Math.toRadians(186)));
        float endY5 = (float) (acy +  (ar + dp(55)) * Math.sin(Math.toRadians(186)));
        float currentX5 = (1 - value5) * (1 - value5) * startX
                + 2 * (1 - value5) * value5 * midX5
                + value5 * value5 * endX5;
        float currentY5 = (1 - value5) * (1 - value5) * startY
                + 2 * (1 - value5) * value5 * midY5
                + value5 * value5 * endY5;

        float midX6 = acx +  (ar + dp(45)) * (float) Math.cos(Math.toRadians(5));
        float midY6 = acy + (ar + dp(45)) * (float) Math.sin(Math.toRadians(5));
        float endX6 = (float) (acx +  (ar + dp(65)) * Math.cos(Math.toRadians(-5)));
        float endY6 = (float) (acy +  (ar + dp(65)) * Math.sin(Math.toRadians(-5)));
        float currentX6 = (1 - value6) * (1 - value6) * startX
                + 2 * (1 - value6) * value6 * midX6
                + value6 * value6 * endX6;
        float currentY6 = (1 - value6) * (1 - value6) * startY
                + 2 * (1 - value6) * value6 * midY6
                + value6 * value6 * endY6;


        for (int i = 0; i < gifts.size(); ++i) {
            final Gift gift = gifts.get(i);
            final float alpha = gift.animatedFloat.set(1.0f);
            final float scale = lerp(0.5f, 1.0f, alpha);
            final int index = i; // gifts.size() == maxCount ? i - 1 : i;
            if (index == 0) {
                gift.draw(
                        canvas,
                        currentX1,
                        currentY1,
                        scale * scale1, -65 + 90,
                        alpha * alpha * (1.0f - expandProgress) * (1.0f - actionBarProgress) * (closedAlpha),
                        1.0f
                );
            } else if (index == 1) {
                gift.draw(
                        canvas,
                        currentX2,
                        currentY2,
                        scale* scale2, -4.0f,
                        alpha * alpha * (1.0f - expandProgress) * (1.0f - actionBarProgress) * (closedAlpha),
                        1.0f
                );
            } else if (index == 2) {
                gift.draw(
                        canvas,
                        currentX3,
                        currentY3,
                        scale * scale3, 8.0f,
                        alpha * (1.0f - expandProgress) * (1.0f - actionBarProgress) * (closedAlpha),
                        1.0f
                );
            } else if (index == 3) {
                gift.draw(
                        canvas,
                        currentX4,
                        currentY4,
                        scale * scale4, 3.0f,
                        alpha * (1.0f - expandProgress) * (1.0f - actionBarProgress) * (closedAlpha),
                        1.0f
                );
            } else if (index == 4) {
                gift.draw(
                        canvas,
                        currentX5,
                        currentY5,
                        scale * scale5, -3.0f,
                        alpha * (1.0f - expandProgress) * (1.0f - actionBarProgress) * (closedAlpha),
                        1.0f
                );
            } else if (index == 5) {
                gift.draw(
                        canvas,
                        currentX6,
                        currentY6,
                        scale * scale6, 2.0f,
                        alpha * (1.0f - expandProgress) * (1.0f - actionBarProgress) * (closedAlpha),
                        1.0f
                );
            }
        }

        canvas.restore();
    }

    public Gift getGiftUnder(float x, float y) {
        for (int i = 0; i < gifts.size(); ++i) {
            if (gifts.get(i).bounds.contains(x, y))
                return gifts.get(i);
        }
        return null;
    }

    private Gift pressedGift;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final Gift hit = getGiftUnder(event.getX(), event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            pressedGift = hit;
            if (pressedGift != null) {
                pressedGift.bounce.setPressed(true);
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressedGift != hit && pressedGift != null) {
                pressedGift.bounce.setPressed(false);
                pressedGift = null;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (pressedGift != null) {
                onGiftClick(pressedGift);
                pressedGift.bounce.setPressed(false);
                pressedGift = null;
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (pressedGift != null) {
                pressedGift.bounce.setPressed(false);
                pressedGift = null;
            }
        }
        return pressedGift != null;
    }

    public void onGiftClick(Gift gift) {
        Browser.openUrl(getContext(), "https://t.me/nft/" + gift.slug);
    }
}
