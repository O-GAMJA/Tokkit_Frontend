// ========= Genie 어댑터 파일 =========
package com.example.tokkit.genie

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.R

class MessageRecyclerViewAdapter(
    private val context: Context,
    private val messages: ArrayList<ChatMessage>
) : RecyclerView.Adapter<MessageRecyclerViewAdapter.MyViewHolder>() {

    // 삭제 콜백 인터페이스 정의
    interface OnMessageDeleteListener {
        fun onMessageDelete(position: Int)
    }

    private var messageDeleteListener: OnMessageDeleteListener? = null

    fun setOnMessageDeleteListener(listener: OnMessageDeleteListener) {
        this.messageDeleteListener = listener
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userMessage: TextView = view.findViewById(R.id.user_message)
        val botMessage: TextView = view.findViewById(R.id.bot_message)
        val botImage: ImageView = view.findViewById(R.id.bot_image)
        val leftChatLayout: LinearLayout = view.findViewById(R.id.left_chat_layout)
        val rightChatLayout: LinearLayout = view.findViewById(R.id.right_chat_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.genie_chat_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val msg = messages[position]
        if (msg.isMessageFromUser()) {
            holder.userMessage.text = msg.getMessage()
            holder.leftChatLayout.visibility = View.GONE
            holder.rightChatLayout.visibility = View.VISIBLE
        } else {
            holder.botMessage.text = msg.getMessage()
            holder.leftChatLayout.visibility = View.VISIBLE
            holder.rightChatLayout.visibility = View.GONE
            holder.botImage.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
    }

    fun updateBotMessage(botMessage: String): String {
        var lastMessageFromBot = false
        var lastMessage: ChatMessage? = null

        if (messages.size > 1) {
            lastMessage = messages[messages.size - 1]
            if (lastMessage.mSender == MessageSender.BOT) {
                lastMessageFromBot = true
            }
        } else {
            addMessage(ChatMessage(botMessage, MessageSender.BOT))
        }

        if (lastMessageFromBot) {
            messages[messages.size - 1].mMessage = messages[messages.size - 1].mMessage + botMessage
        } else {
            addMessage(ChatMessage(botMessage, MessageSender.BOT))
        }

        return messages[messages.size - 1].mMessage
    }

    // 메시지 삭제 함수 추가
    fun removeMessage(position: Int) {
        if (position >= 0 && position < messages.size) {
            messages.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}