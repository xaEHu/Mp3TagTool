package com.xaehu.mp3tagtool

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import kotlinx.android.synthetic.main.adapter_item.view.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.image


class MyListAdapter(private val data: MutableList<SongBean>) :
    RecyclerView.Adapter<MyListAdapter.MyViewHolder>() {
    private val TAG = "MyListAdapter"
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.adapter_item,parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemView.apply {
            tv1.text = data[position].name
            tv2.text = data[position].singer
            val path = data[position].path
            tv3.text = path
            tv3.isSelected = true
            if(path!!.startsWith("http")){
                Log.i(TAG, "load_picture:${path}")
                img.visibility = View.VISIBLE
                img.load(path){
                    crossfade(true)
                }
            }else{
                val artBitmap = data[position].getArtBitmap()
                if(artBitmap != null){
                    img.visibility = View.VISIBLE
                    img.setImageBitmap(artBitmap)
                }else{
                    img.visibility = View.GONE
                }
            }
            img.setOnClickListener{
                val dialogImg = ImageView(context)
                val layoutParams = FrameLayout
                    .LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER)
                dialogImg.layoutParams = layoutParams
                dialogImg.adjustViewBounds = true
                dialogImg.image = img.image
                context.alert{
                    negativeButton("关闭") {}
                    customView = dialogImg
                }.show()
            }
            setOnClickListener{
                listener?.setOnItemClickListener(data[position],position)
            }
        }
    }

    override fun getItemCount(): Int = data.size

    private var listener:OnListener? = null
    fun setListener(listener:OnListener){
        this.listener = listener
    }

    interface OnListener{
        fun setOnItemClickListener(bean: SongBean,position: Int)
    }

    fun setNewData(newData: List<SongBean>){
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }
}