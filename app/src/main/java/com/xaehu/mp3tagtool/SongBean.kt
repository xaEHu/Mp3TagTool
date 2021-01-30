package com.xaehu.mp3tagtool

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Parcel
import android.os.Parcelable
import android.util.Log

class SongBean() :Parcelable{
    var name:String? = null
    var singer:String? = null
    var path:String? = null

    constructor(parcel: Parcel) : this() {
        name = parcel.readString()
        singer = parcel.readString()
        path = parcel.readString()
    }

    constructor(name: String?, singer: String?, path: String?) : this(){
        this.name = name
        this.singer = singer
        this.path = path
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(singer)
        parcel.writeString(path)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SongBean> {
        override fun createFromParcel(parcel: Parcel): SongBean {
            return SongBean(parcel)
        }

        override fun newArray(size: Int): Array<SongBean?> {
            return arrayOfNulls(size)
        }
    }

    fun getArtBitmap(): Bitmap? {
        val myRetriever = MediaMetadataRetriever()
        try {
            myRetriever.setDataSource(path) // the URI of audio file
        } catch (e: Exception) {
            Log.e("error", "getArtBitmapError: $e")
            return null
        }
        val artwork = myRetriever.embeddedPicture
        return if (artwork != null) {
            BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
        } else {
            null
        }
    }
}