package com.example.helloandroidapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView edtResult;
    private HorizontalScrollView resultScroll;
    private String expression = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtResult = findViewById(R.id.edtResult);
        resultScroll = findViewById(R.id.resultScroll);

        if (savedInstanceState != null) {
            expression = savedInstanceState.getString("expression", "0");
        }
        applyDisplay(expression);

        int[] inputIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv, R.id.btnDot
        };

        View.OnClickListener appendListener = v -> {
            String t = ((Button) v).getText().toString();
            if (expression.equals("0") && !t.equals(".")) {
                if (!isOperatorString(t)) expression = "";
            }
            if (isOperatorString(t)) {
                if (expression.isEmpty()) return;
                if (isOperatorChar(lastCharSafe())) {
                    expression = expression.substring(0, expression.length() - 1) + t;
                } else {
                    expression += t;
                }
            } else if (t.equals(".")) {
                if (canAppendDot(expression)) expression += t;
            } else {
                expression += t;
            }
            applyDisplay(expression);
        };

        for (int id : inputIds) {
            findViewById(id).setOnClickListener(appendListener);
        }

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            expression = "0";
            applyDisplay(expression);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (expression.length() <= 1) {
                expression = "0";
            } else {
                expression = expression.substring(0, expression.length() - 1);
                if (expression.isEmpty()) expression = "0";
            }
            applyDisplay(expression);
        });

        findViewById(R.id.btnEqual).setOnClickListener(v -> {
            if (expression.isEmpty()) {
                expression = "0";
                applyDisplay(expression);
                return;
            }
            if (isOperatorChar(lastCharSafe())) {
                expression = expression.substring(0, expression.length() - 1);
                if (expression.isEmpty()) {
                    expression = "0";
                    applyDisplay(expression);
                    return;
                }
            }
            try {
                String calcExpr = expression.replace("X", "*");
                double result = evaluate(calcExpr);
                String out = (result == Math.rint(result)) ? String.valueOf((long) result) : String.valueOf(result);
                expression = out;
                applyDisplay(expression);
            } catch (ArithmeticException ex) {
                expression = "0";
                edtResult.setText("Math Error");
                edtResult.setContentDescription("Lỗi: Math Error");
            } catch (Exception ex) {
                expression = "0";
                edtResult.setText("Error");
                edtResult.setContentDescription("Lỗi: Error");
            }
        });
    }

    private void applyDisplay(String text) {
        if (text == null || text.isEmpty()) text = "0";
        edtResult.setText(text);
        edtResult.setContentDescription("Kết quả: " + text);
        // Tự động cuộn về cuối
        resultScroll.post(() -> resultScroll.fullScroll(View.FOCUS_RIGHT));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("expression", expression);
    }

    private boolean isOperatorString(String s) {
        return s.equals("+") || s.equals("-") || s.equals("/") || s.equals("X");
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '/' || c == 'X' || c == '*';
    }

    private char lastCharSafe() {
        if (expression == null || expression.isEmpty()) return '\0';
        return expression.charAt(expression.length() - 1);
    }

    private boolean canAppendDot(String expr) {
        if (expr.isEmpty()) return false;
        int i = expr.length() - 1;
        while (i >= 0 && !isOperatorChar(expr.charAt(i))) {
            if (expr.charAt(i) == '.') return false;
            i--;
        }
        return true;
    }

    // ===== Evaluate: tokenize -> RPN -> evalRPN =====
    private double evaluate(String expr) {
        List<String> rpn = toRPN(expr);
        return evalRPN(rpn);
    }

    private List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (isNumberChar(c)) {
                num.append(c);
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (num.length() > 0) {
                    tokens.add(num.toString());
                    num.setLength(0);
                }
                tokens.add(String.valueOf(c));
            }
        }
        if (num.length() > 0) tokens.add(num.toString());
        return tokens;
    }

    private boolean isNumberChar(char c) {
        return (c >= '0' && c <= '9') || c == '.';
    }

    private int precedence(String op) {
        if (op.equals("+") || op.equals("-")) return 1;
        if (op.equals("*") || op.equals("/")) return 2;
        return 0;
    }

    private List<String> toRPN(String expr) {
        List<String> tokens = tokenize(expr);
        List<String> output = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();
        for (String t : tokens) {
            char c0 = t.charAt(0);
            if (Character.isDigit(c0) || c0 == '.') {
                output.add(t);
            } else {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(t)) {
                    output.add(ops.pop());
                }
                ops.push(t);
            }
        }
        while (!ops.isEmpty()) output.add(ops.pop());
        return output;
    }

    private double evalRPN(List<String> rpn) {
        Deque<Double> st = new ArrayDeque<>();
        for (String t : rpn) {
            if (t.equals("+") || t.equals("-") || t.equals("*") || t.equals("/")) {
                if (st.size() < 2) throw new IllegalArgumentException("Invalid expression");
                double b = st.pop();
                double a = st.pop();
                double r;
                switch (t) {
                    case "+": r = a + b; break;
                    case "-": r = a - b; break;
                    case "*": r = a * b; break;
                    case "/":
                        if (b == 0.0) throw new ArithmeticException("Divide by zero");
                        r = a / b; break;
                    default: throw new IllegalArgumentException("Unknown op");
                }
                st.push(r);
            } else {
                st.push(Double.parseDouble(t));
            }
        }
        if (st.size() != 1) throw new IllegalArgumentException("Invalid expression");
        return st.pop();
    }
}
