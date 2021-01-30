package com.xaehu.mp3tagtool

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart.setOnClickListener{
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),1000)
            }else{
                startActivity(Intent(this,ListActivity::class.java))
            }
        }

        githubUrl.setOnClickListener {
            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            intent.data = Uri.parse("https://github.com/xaEHu/Mp3TagTool")
            startActivity(intent)
        }

        btnAddQQGroup.setOnClickListener {
            joinQQGroup()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1000){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startActivity(Intent(this,ListActivity::class.java))
            }
        }
    }

    /****************
     * 发起添加群流程。群号：VipMusicDownload(962444674) 的 key 为： tUQwB_0XP4nDGjC4LLPhmhWNc7yigqLa
     * 调用 joinQQGroup(tUQwB_0XP4nDGjC4LLPhmhWNc7yigqLa) 即可发起手Q客户端申请加群 VipMusicDownload(962444674)
     *
     * @return 返回true表示呼起手Q成功，返回fals表示呼起失败
     */
    fun joinQQGroup(): Boolean {
        val intent = Intent()
        intent.data =
            Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3DtUQwB_0XP4nDGjC4LLPhmhWNc7yigqLa")
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            startActivity(intent)
            true
        } catch (e: Exception) {
            // 未安装手Q或安装的版本不支持
            Toast.makeText(this, "你没有安装QQ哎", Toast.LENGTH_SHORT).show()
            false
        }
    }
}