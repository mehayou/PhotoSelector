package com.mehayou.photoselector.result;

import android.os.AsyncTask;

import java.io.File;

class ResultAsyncTask<T> extends AsyncTask<Void, Void, T> {

    private Result<T> result;
    private File file;

    ResultAsyncTask(Result<T> result, File file) {
        this.result = result;
        this.file = file;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected T doInBackground(Void... voids) {
        return this.result.onImageResult(this.file);
    }

    @Override
    protected void onPostExecute(T t) {
        super.onPostExecute(t);
        this.result.callback(t);
    }
}
