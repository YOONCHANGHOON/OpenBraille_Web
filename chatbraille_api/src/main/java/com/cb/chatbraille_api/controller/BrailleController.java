package com.cb.chatbraille_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/braille")
public class BrailleController {

    // 영어 점자 → 영어 텍스트 변환
    private String brailleToEnglish(String brailleData) {
        // TODO: 실제 점자 해석 로직
        // 임시로 그냥 "hello" 반환
        return "hello";
    }

    // 영어 텍스트 → 한글 텍스트 변환 (예: 사전 매핑, 번역 API 등)
    private String englishToKorean(String englishText) {
        if ("hello".equalsIgnoreCase(englishText)) {
            return "안녕";
        }
        return englishText; // 기본은 그대로 리턴
    }

    // 한글 텍스트 → 한글 점자 변환 (Liblouis CLI 호출 등)
    private String koreanToBraille(String koreanText) {
        try {
            // 명령어: echo "안녕" | lou_translate ko-g1.ctb
            ProcessBuilder builder = new ProcessBuilder(
                    "bash", "-c", "echo \"" + koreanText + "\" | lou_translate ko-g1.ctb"
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();

            return output.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "점자 변환 오류";
        }
    }

    // ⠟ 같은 점자 유니코드 문자 → 6점 배열로 변환
    private int[] brailleCharToDots(char brailleChar) {
        int dots = brailleChar - 0x2800; // 점자 유니코드 시작값
        int[] result = new int[6];
        for (int i = 0; i < 6; i++) {
            result[i] = (dots & (1 << i)) != 0 ? 1 : 0;
        }
        return result;
    }

    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertBraille(@RequestBody Map<String, String> request) {
        String englishBrailleData = request.get("brailleData");
        if (englishBrailleData == null || englishBrailleData.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "brailleData is required"));
        }

        // 1) 영어 점자 → 영어 텍스트
        String englishText = brailleToEnglish(englishBrailleData);

        // 2) 영어 텍스트 → 한글 텍스트
        String koreanText = englishToKorean(englishText);

        // 3) 한글 텍스트 → 한글 점자 (유니코드 문자)
        String koreanBraille = koreanToBraille(koreanText);

        // 4) 점자 문자열 → 점자 배열로 변환
        List<int[]> brailleArray = new java.util.ArrayList<>();
        for (char ch : koreanBraille.toCharArray()) {
            brailleArray.add(brailleCharToDots(ch));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("koreanText", koreanText);
        response.put("koreanBraille", koreanBraille);
        response.put("koreanBrailleArray", brailleArray);

        return ResponseEntity.ok(response);
    }
}