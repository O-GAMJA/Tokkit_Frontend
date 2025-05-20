// ---------------------------------------------------------------------
// Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
// SPDX-License-Identifier: BSD-3-Clause
// ---------------------------------------------------------------------
#include "PromptHandler.hpp"
#include "GenieWrapper.hpp"

using namespace AppUtils;

// Llama3 prompt
constexpr const std::string_view c_bot_name = "Tokkit";
constexpr const std::string_view c_first_prompt_prefix_part_1 =
        "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\nYour name is ";
constexpr const std::string_view c_first_prompt_prefix_part_2 =
        " and you are a helpful AI assistant. Please keep answers concise and to the point.";
constexpr const std::string_view c_ocr_reference_prefix = "\n\nRefer to the following study note content when answering questions:\n\n";
constexpr const std::string_view c_prompt_prefix = "<|start_header_id|>user<|end_header_id|>\n\n";
constexpr const std::string_view c_end_of_prompt = "<|eot_id|>";
constexpr const std::string_view c_assistant_header = "<|start_header_id|>assistant<|end_header_id|>\n\n";

PromptHandler::PromptHandler()
        : m_is_first_prompt(true), m_ocr_text("")
{
}

void PromptHandler::SetOcrText(const std::string& ocr_text)
{
    m_ocr_text = ocr_text;
}

std::string PromptHandler::GetPromptWithTag(const std::string& user_prompt)
{
    // Ref: https://www.llama.com/docs/model-cards-and-prompt-formats/meta-llama-3/
    if (m_is_first_prompt)
    {
        m_is_first_prompt = false;
        std::string system_prompt = std::string(c_first_prompt_prefix_part_1) + c_bot_name.data() + c_first_prompt_prefix_part_2.data();

        // OCR 텍스트가 있는 경우 시스템 프롬프트에 추가
        if (!m_ocr_text.empty())
        {
            system_prompt += c_ocr_reference_prefix.data() + m_ocr_text;
        }

        system_prompt += " <|eot_id|>";

        return system_prompt + c_prompt_prefix.data() + user_prompt + c_end_of_prompt.data() + c_assistant_header.data();
    }
    return std::string(c_prompt_prefix) + user_prompt.data() + c_end_of_prompt.data() + c_assistant_header.data();
}