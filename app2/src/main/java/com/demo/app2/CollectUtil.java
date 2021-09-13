package com.demo.app2;

import java.util.List;

public class CollectUtil {

    public interface Executor<T> {
        void execute(T t);
    }

    public static <T> void execute(List<T> list, Executor<T> executor) {
        if (list == null || list.isEmpty() || executor == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            executor.execute(list.get(i));
        }
    }

}
