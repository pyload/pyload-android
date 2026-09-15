package org.pyload.android.client.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ExpandableListView;

public class DragExpandableListView extends ExpandableListView {

    public interface OnItemMovedListener {
        void onGroupMoved(int from, int to);
        void onChildMoved(int group, int from, int to);
        void onDragStopped();
    }

    private boolean dragEnabled = false;
    private boolean reorderMode = false;
    private boolean isScrolling = false;
    private float touchDownX, touchDownY;

    private OnItemMovedListener movedListener;
    private int dragStartPos;
    private int dragStartGroup;
    private int dragStartChild;

    private View dragView;
    private Bitmap dragBitmap;
    private int dragPos; // Flat position
    private int dragGroup = -1;
    private int dragChild = -1;
    private boolean isGroupDrag = false;

    private int dragHoverY; // Current touch Y in list coords
    private int dragTouchOffset; // Offset from touch Y to item top

    private final Paint paint = new Paint();

    public DragExpandableListView(Context context) {
        super(context);
        paint.setAlpha(180); // Slight transparency for the floating item
    }

    public DragExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAlpha(180);
    }

    public DragExpandableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setAlpha(180);
    }

    public DragExpandableListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        paint.setAlpha(180);
    }

    public void setOnItemMovedListener(OnItemMovedListener listener) {
        this.movedListener = listener;
    }

    public void setReorderMode(boolean reorder) {
        this.reorderMode = reorder;
        setHapticFeedbackEnabled(!reorder);
    }

    @Override
    public boolean performHapticFeedback(int feedbackConstant) {
        if (reorderMode) {
            return false;
        }
        return super.performHapticFeedback(feedbackConstant);
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

    @Override
    public boolean showContextMenu() {
        if (reorderMode) {
            return true;
        }
        return super.showContextMenu();
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        if (reorderMode) {
            return true;
        }
        return super.showContextMenuForChild(originalView);
    }

    @Override
    public boolean showContextMenuForChild(View originalView, float x, float y) {
        if (reorderMode) {
            return true;
        }
        return super.showContextMenuForChild(originalView, x, y);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (reorderMode && !dragEnabled) {
            int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchDownX = ev.getX();
                    touchDownY = ev.getY();
                    isScrolling = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.hypot(ev.getX() - touchDownX, ev.getY() - touchDownY) > touchSlop) {
                        isScrolling = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!isScrolling) {
                        return true;
                    }
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void startDrag(int position, float rawY) {
        long packedPos = getExpandableListPosition(position);
        int group = getPackedPositionGroup(packedPos);
        int child = getPackedPositionChild(packedPos);

        if (group == -1) return;

        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        dragStartPos = position;
        dragStartGroup = group;
        dragStartChild = child;

        dragPos = position;
        dragGroup = group;
        dragChild = child;
        isGroupDrag = (child == -1);

        if (isGroupDrag && isGroupExpanded(group)) {
            collapseGroup(group);
        }

        dragView = getChildAt(position - getFirstVisiblePosition());
        if (dragView == null || dragView.getWidth() <= 0 || dragView.getHeight() <= 0) return;

        dragBitmap = Bitmap.createBitmap(dragView.getWidth(), dragView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dragBitmap);
        dragView.draw(canvas);

        int[] listCoords = new int[2];
        getLocationOnScreen(listCoords);
        dragHoverY = (int) (rawY - listCoords[1]);
        dragTouchOffset = dragHoverY - dragView.getTop();
        
        dragEnabled = true;
        invalidate();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (reorderMode && !dragEnabled) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!isScrolling) {
                        isScrolling = false;
                        return true;
                    }
                    isScrolling = false;
                    break;
            }
        }
        if (dragEnabled) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    int y = (int) ev.getY();
                    dragHoverY = y;
                    
                    int targetPos = pointToPosition((int)ev.getX(), y);
                    if (targetPos != INVALID_POSITION) {
                        checkSwap(targetPos);
                    }
                    
                    checkScroll(y);
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

    private void checkScroll(int y) {
        int height = getHeight();
        int scrollBound = height / 5;
        if (y < scrollBound) {
            smoothScrollBy(-20, 0);
        } else if (y > height - scrollBound) {
            smoothScrollBy(20, 0);
        }
    }

    private void checkSwap(int targetPos) {
        if (targetPos == dragPos) return;

        long targetPacked = getExpandableListPosition(targetPos);
        int targetGroup = getPackedPositionGroup(targetPacked);
        int targetChild = getPackedPositionChild(targetPacked);

        if (isGroupDrag) {
            // Ensure target is a group and not dragging inside an expanded package
            if (targetChild == -1 && targetGroup != -1 && targetGroup != dragGroup) {
                if (movedListener != null) {
                    movedListener.onGroupMoved(dragGroup, targetGroup);
                    dragGroup = targetGroup;
                    dragPos = targetPos;
                }
            }
        } else {
            // Ensure target is a child within the SAME group
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
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        dragView = null;
        if (dragBitmap != null) {
            dragBitmap.recycle();
            dragBitmap = null;
        }
        invalidate();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (dragEnabled) {
            int position = getPositionForView(child);
            if (position == dragPos) {
                // This is the gap, don't draw the item
                return true;
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (dragEnabled && dragBitmap != null) {
            int x = 0;
            // Draw floating item at touch position with proper offset
            int y = dragHoverY - dragTouchOffset;
            canvas.drawBitmap(dragBitmap, x, y, paint);
        }
    }
}
