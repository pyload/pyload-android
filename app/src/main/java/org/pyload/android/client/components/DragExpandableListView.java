package org.pyload.android.client.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

public class DragExpandableListView extends ExpandableListView {

    public interface OnItemMovedListener {
        void onGroupMoved(int from, int to);
        void onChildMoved(int group, int from, int to);
        void onDragStopped();
    }

    private boolean dragEnabled = false;
    private OnItemMovedListener movedListener;
    private int dragStartPos;
    private int dragStartGroup;
    private int dragStartChild;

    private View dragView;
    private Bitmap dragBitmap;
    private int dragStartRawY;
    private int dragOffset;
    private int dragPos; // Flat position
    private int dragGroup = -1;
    private int dragChild = -1;
    private boolean isGroupDrag = false;

    private final Paint paint = new Paint();

    public DragExpandableListView(Context context) {
        super(context);
    }

    public DragExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DragExpandableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DragExpandableListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setOnItemMovedListener(OnItemMovedListener listener) {
        this.movedListener = listener;
    }

    public int getDragGroup() {
        return dragGroup;
    }

    public int getDragChild() {
        return dragChild;
    }

    public boolean isGroupDrag() {
        return isGroupDrag;
    }

    public void startDrag(int position, View handle) {
        long packedPos = getExpandableListPosition(position);
        int group = getPackedPositionGroup(packedPos);
        int child = getPackedPositionChild(packedPos);

        if (group == -1) return;

        dragStartPos = position;
        dragStartGroup = group;
        dragStartChild = child;

        dragPos = position;
        dragGroup = group;
        dragChild = child;
        isGroupDrag = (child == -1);

        dragView = getChildAt(position - getFirstVisiblePosition());
        if (dragView == null || dragView.getWidth() <= 0 || dragView.getHeight() <= 0) return;

        dragBitmap = Bitmap.createBitmap(dragView.getWidth(), dragView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dragBitmap);
        dragView.draw(canvas);

        Rect rect = new Rect();
        if (!handle.getGlobalVisibleRect(rect)) return;
        dragStartRawY = rect.centerY();
        
        dragOffset = 0;
        dragEnabled = true;
        dragView.setVisibility(View.INVISIBLE);
        invalidate();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (dragEnabled) {
            int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    int y = (int) ev.getY();
                    int rawY = (int) ev.getRawY();
                    dragOffset = rawY - dragStartRawY;
                    
                    int targetPos = pointToPosition((int)ev.getX(), y);
                    if (targetPos != INVALID_POSITION) {
                        checkSwap(targetPos);
                    }
                    
                    invalidate();
                    return true;

                case MotionEvent.ACTION_UP:
                    performClick();
                    stopDrag();
                    if (movedListener != null) {
                        movedListener.onDragStopped();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    stopDrag();
                    if (movedListener != null) {
                        movedListener.onDragStopped();
                    }
                    return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    private void checkSwap(int targetPos) {
        if (targetPos == dragPos) return;

        long targetPacked = getExpandableListPosition(targetPos);
        int targetGroup = getPackedPositionGroup(targetPacked);
        int targetChild = getPackedPositionChild(targetPacked);

        if (isGroupDrag) {
            if (targetChild == -1 && targetGroup != -1 && targetGroup != dragGroup) {
                if (movedListener != null) {
                    movedListener.onGroupMoved(dragGroup, targetGroup);
                    dragGroup = targetGroup;
                    dragPos = targetPos;
                }
            }
        } else {
            if (targetGroup == dragGroup && targetChild != -1 && targetChild != dragChild) {
                if (movedListener != null) {
                    movedListener.onChildMoved(dragGroup, dragChild, targetChild);
                    dragChild = targetChild;
                    dragPos = targetPos;
                }
            }
        }
    }

    private void stopDrag() {
        dragEnabled = false;
        if (dragView != null) {
            dragView.setVisibility(View.VISIBLE);
        }
        dragView = null;
        if (dragBitmap != null) {
            dragBitmap.recycle();
            dragBitmap = null;
        }
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (dragEnabled && dragBitmap != null && dragView != null) {
            int x = 0;
            int y = dragView.getTop() + dragOffset;
            canvas.drawBitmap(dragBitmap, x, y, paint);
        }
    }
}
