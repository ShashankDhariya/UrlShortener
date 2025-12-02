package com.example.Url.Shortener.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UrlRequestDTO {
    private String url;
    private String custom;
}
