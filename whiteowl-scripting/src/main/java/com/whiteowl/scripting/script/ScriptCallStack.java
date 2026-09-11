package com.whiteowl.scripting.script;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScriptCallStack {

    private static final ThreadLocal<Deque<String>> CALL_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    public static void push(String scriptName) {
        Deque<String> stack = CALL_STACK.get();
        if (stack.contains(scriptName)) {
            List<String> chain = new ArrayList<>(stack);
            chain.add(scriptName);
            throw new ScriptCycleException(chain);
        }
        stack.push(scriptName);
    }

    public static void pop() {
        Deque<String> stack = CALL_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static void clear() {
        CALL_STACK.get().clear();
    }

}
