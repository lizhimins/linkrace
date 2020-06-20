package com.alirace.util;

public class NumberUtil {

    public static long combineInt2Long(int high, int low) {
        return (((long) high << 32) & 0xFFFFFFFF00000000L) | ((long) low & 0xFFFFFFFFL);
    }

    public static void bubbleSort(long[] time, long[] offset, int length) {
        long temp; int i;
        // 外层循环：n个元素排序，则至多需要 n-1 趟循环
        for (i = 0; i < length - 1; i++) {
            // 定义一个布尔类型的变量，标记数组是否已达到有序状态
            boolean flag = true;
            /*内层循环：每一趟循环都从数列的前两个元素开始进行比较，比较到无序数组的最后*/
            for (int j = 0; j < length - 1; j++) {
                // 如果前一个元素大于后一个元素，则交换两元素的值；
                if (time[j] > time[j + 1]) {
                    temp = time[j];
                    time[j] = time[j + 1];
                    time[j + 1] = temp;

                    temp = offset[j];
                    offset[j] = offset[j + 1];
                    offset[j + 1] = temp;
                    //本趟发生了交换，表明该数组在本趟处于无序状态，需要继续比较； 即本躺只要发生了一次交换，就false
                    flag = false;
                }
            }
            //根据标记量的值判断数组是否有序，如果有序，则退出；无序，则继续循环。
            if (flag) {
                break;
            }
        }
    }
}
