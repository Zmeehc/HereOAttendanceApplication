package com.llavore.hereoattendance.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class TermsScrollView extends ScrollView {
    
    private OnScrollToBottomListener onScrollToBottomListener;
    
    public interface OnScrollToBottomListener {
        void onScrollToBottom();
    }
    
    public TermsScrollView(Context context) {
        super(context);
    }
    
    public TermsScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public TermsScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    public void setOnScrollToBottomListener(OnScrollToBottomListener listener) {
        this.onScrollToBottomListener = listener;
    }
    
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        
        // Check if scrolled to bottom
        if (getChildAt(0) != null) {
            int childHeight = getChildAt(0).getHeight();
            int scrollY = getScrollY();
            int height = getHeight();
            
            // Consider scrolled to bottom if within 50 pixels of the bottom
            if (scrollY + height >= childHeight - 50) {
                if (onScrollToBottomListener != null) {
                    onScrollToBottomListener.onScrollToBottom();
                }
            }
        }
    }
}
