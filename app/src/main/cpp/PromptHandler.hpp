//
// Created by Bhushan Sonawane on 10/6/24.
//

// ---------------------------------------------------------------------
// Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
// SPDX-License-Identifier: BSD-3-Clause
// ---------------------------------------------------------------------
#pragma once

#include <string>

namespace AppUtils
{

class PromptHandler
{
  private:
    bool m_is_first_prompt;
    std::string m_ocr_text; // OCR 텍스트를 저장하는 필드
    bool m_is_quiz_mode;

public:
    PromptHandler();
    std::string GetPromptWithTag(const std::string& user_prompt);
    void SetOcrText(const std::string& ocr_text); // OCR 텍스트 설정
    void SetQuizMode(bool is_quiz);                // 퀴즈 모드 설정
};

} // namespace AppUtils
