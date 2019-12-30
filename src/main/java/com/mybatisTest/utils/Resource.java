package com.mybatisTest.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Resource {
    private volatile List<String> list = new ArrayList<String>();
    private boolean flag = false;
    private Lock lock = new ReentrantLock();
    //使用lock建立生产者的condition对象
    private Condition condition_pro = lock.newCondition();
    //使用lock建立消费者的condition对象
    private Condition condition_con = lock.newCondition();


    public void writeInto(String time) throws IOException {
        File writename = new File("file/TPS.txt"); // 相对路径，如果没有则要建立一个新的output.txt文件
        writename.createNewFile(); // 创建新文件
        BufferedWriter out = new BufferedWriter(new FileWriter(writename,true));
        out.write(time+"\r\n"); // \r\n即为换行
        out.flush(); // 把缓存区内容压入文件
        out.close(); // 最后记得关闭文件
    }


    /**
     * 生产资源
     * @param f
     */
    public void create(String f) throws InterruptedException {
        lock.lock();
        try{
            //先判断标记是否已经生产了，如果已经生产，等待消费
            while(flag){
                //生产者等待
                condition_pro.await();
            }
            //生产一个
            list.add(f);
            //生产者生产完毕后，唤醒消费者的线程（注意这里不是signalAll)
            if(list.size()==100) {
                flag = true;
                condition_con.signal();
            }
        }finally{
            lock.unlock();
        }
    }

    /**
     * 消费资源
     */
    public void destroy() throws InterruptedException{
        lock.lock();
        try{
            //先判断标记是否已经消费了，如果已经消费，等待生产
            while(!flag){
                //消费者等待
                condition_con.await();
            }
            for(int i = 0;i < list.size();i++) {
                writeInto(list.get(i));
            }
            //将资源标记为已经消费,清空列表
            list.clear();
            flag = false;
            //消费者消费完毕后，唤醒生产者的线程
            condition_pro.signalAll();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            lock.unlock();
        }
    }

}