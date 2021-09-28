package com.tencent.mobility.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.tencent.mobility.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PanelView extends FrameLayout {

    private TextView mEnter;
    private TextView mConsole;
    private final AtomicInteger doActionCount = new AtomicInteger(1);
    private final StringBuffer mConsoleBuffer = new StringBuffer();
    private final List<String> mPanelItemKeys = new ArrayList<>();
    private final Map<String, Action> mActions = new HashMap<>();
    private final HandlerThread mActionThread = new HandlerThread("action_thread") {
        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mActionHandler = new Handler(getLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                }
            };
            synchronized (mActionThread) {
                mActionThread.notifyAll();
            }
        }
    };
    private volatile Handler mActionHandler;

    public PanelView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PanelView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    private void initViews() {
        mEnter = findViewById(R.id.panel_enter);
        mConsole = findViewById(R.id.panel_console);
        mConsole.setMovementMethod(ScrollingMovementMethod.getInstance());
        mConsole.setOnLongClickListener(v -> {
            if (mConsoleBuffer.length() > 0) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(mEnter.getText().toString(), mConsoleBuffer.toString()));
                Toast.makeText(getContext(), "拷贝成功", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        mEnter.setOnClickListener(v -> {
            if (!mPanelItemKeys.isEmpty()) {
                openDialog();
            }
        });
    }

    private void openDialog() {
        final String[] items = mPanelItemKeys.toArray(new String[0]);
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(getContext());
        listDialog.setTitle("操作");
        listDialog.setItems(items, (dialog, which) -> {
            Message message = Message.obtain(mActionHandler, () -> {
                String actionName = items[which];
                doAction(actionName);
            });
            message.sendToTarget();
        });
        listDialog.show();
    }

    public void print(String content) {

        if (!TextUtils.isEmpty(content)) {
            mConsoleBuffer.append(doActionCount.getAndIncrement()).append(".")
                    .append(content).append("\n");
            updateConsole();
        }
    }

    public void postPrint(String content) {
        Message message = Message.obtain(mActionHandler, () -> {
            print(content);
        });
        message.sendToTarget();
    }

    private void doAction(String actionName) {
        Action action = mActions.get(actionName);
        if (action != null) {
            Object ret = action.run();
            if (ret != action.value) {
                action.setValue(ret);
                mConsoleBuffer.append(doActionCount.getAndIncrement()).append(".")
                        .append(actionName).append(":").append(action.value).append("\n");
            } else {
                mConsoleBuffer.append(doActionCount.getAndIncrement()).append(".")
                        .append(actionName).append(":").append("执行失败").append("\n");
            }

            updateConsole();
        }
    }

    private void updateConsole() {
        post(() -> {
            mConsole.setText(mConsoleBuffer.toString());

            post(() -> {
                int textHeight = mConsole.getLineHeight() * mConsole.getLineCount();
                int textViewHeight = mConsole.getMeasuredHeight();
                if (textHeight > textViewHeight) {
                    mConsole.scrollTo(0, textHeight - textViewHeight + mConsole.getLineHeight());
                }
            });

        });
    }

    /**
     * 初始化面板
     *
     * @param title   标题
     * @param actions 功能选项名称
     */
    public void init(String title, String... actions) {
        mEnter.setText(title);
        mPanelItemKeys.addAll(Arrays.asList(actions));
        mActionThread.start();
        while (mActionHandler == null) {
            try {
                synchronized (mActionThread) {
                    mActionThread.wait(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        addAction("清理", new Action<Boolean>(false) {
            @Override
            public Boolean run() {
                mConsoleBuffer.delete(0, mConsoleBuffer.length());
                doActionCount.set(0);
                updateConsole();
                return true;
            }
        });
    }

    public <T> void addAction(String key, Action<T> action) {
        mActions.put(key, action);
    }

    public void postAction(String key) {
        Message message = Message.obtain(mActionHandler, () -> {
            doAction(key);
        });
        message.sendToTarget();
    }

    public <T> void postAction(String key, Action<T> action) {
        addAction(key, action);
        postAction(key);
    }

    public static class Action<T> {

        private T value;

        public Action(T def) {
            this.value = def;
        }

        public T run() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }
}
