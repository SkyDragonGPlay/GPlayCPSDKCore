package org.cocos2dx.lib;

public final class Cocos2dxDownloader {

    void onProgress(final int id, final long downloadBytes, final long downloadNow, final long downloadTotal) {}

    public void onStart(int id) {}

    public void onFinish(final int id, final int errCode, final String errStr, final byte[] data) {}

    public static Cocos2dxDownloader createDownloader(int id, int timeoutInSeconds, String tempFileNameSufix) {
        return new Cocos2dxDownloader();
    }

    public static Cocos2dxDownloader createDownloader(int id, int timeoutInSeconds, String tempFileNameSufix, int countOfMaxProcessingTasks) {
        return new Cocos2dxDownloader();
    }

    public static void createTask(final Cocos2dxDownloader downloader, int id_, String url_, String path_) {

    }

    public static void cancelAllRequests(final Cocos2dxDownloader downloader) {

    }

    native void nativeOnProgress(int id, int taskId, long dl, long dlnow, long dltotal);
    native void nativeOnFinish(int id, int taskId, int errCode, String errStr, final byte[] data);
}
