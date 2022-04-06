package org.example.generator.config;

import lombok.Data;

import java.util.List;

@Data
public class Modules {

	private String name;

	private List<String> tables;

	private List<View> views;

}