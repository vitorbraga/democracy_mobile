package com.democracy;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.democracy.dto.AnswerOutputDTO;
import com.democracy.dto.QuestionAvailableOutputDTO;
import com.democracy.helper.ConnectionHelper;
import com.democracy.helper.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MainActivity extends AppCompatActivity {

	private Context mContext;

	private ListView listview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.mContext = getApplicationContext();

		this.listview = (ListView) this.findViewById(R.id.listview);

		new GetAvailableQuestionsTask(getApplicationContext()).execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	class GetAvailableQuestionsTask extends AsyncTask<String, String, String> {

		private Context context;

		public GetAvailableQuestionsTask(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			ConnectionHelper.checkInternetConnection(context);
		}

		@Override
		protected String doInBackground(String... arg0) {

			InputStream inputStream = null;
			String result = null;
			try {
				String url = Constants.SERVER_URL
						+ Constants.URL_GET_AVAILABLE_QUESTIONS;

				SharedPreferences prefs = context.getSharedPreferences(
						"com.democracy", Context.MODE_PRIVATE);
				String token = prefs.getString(Constants.TOKEN_SP_KEY, null);

				url = url.replace("<TOKEN>", token);

				HttpURLConnection conn = ConnectionHelper.getConnection(url,
						"GET");

				int statusCode = conn.getResponseCode();

				/* 200 represents HTTP OK */
				if (statusCode == 200) {
					inputStream = new BufferedInputStream(conn.getInputStream());
					result = ConnectionHelper
							.convertInputStreamToString(inputStream);
				} else {
					result = null; // "Failed to fetch data!";
				}

				if (inputStream != null) {
					inputStream.close();
				}

			} catch (Exception e) {
				return new String("Exception: " + e.getMessage());
			}

			return result;
		}

		@Override
		protected void onPostExecute(String result) {

			if (result != null) {
				Gson gson = new Gson();
				ArrayList<QuestionAvailableOutputDTO> questions = gson
						.fromJson(
								result,
								new TypeToken<ArrayList<QuestionAvailableOutputDTO>>() {
								}.getType());

				// Create adapter
				AvailableQuestionsListAdaptor adaptor = new AvailableQuestionsListAdaptor(
						context, R.layout.question_list_item, questions);
				listview.setAdapter(adaptor);
			}
		}

	}

	class AvailableQuestionsListAdaptor extends
			ArrayAdapter<QuestionAvailableOutputDTO> {

		boolean isFirstRun = true;
		
		private ArrayList<QuestionAvailableOutputDTO> questions;

		private Context context;
		
		private HashMap<Integer, String> answersIds = new HashMap<Integer, String>();

		private int allAnswers = 0;
		
		private int countAnswers = 0;

		public AvailableQuestionsListAdaptor(Context context,
				int textViewResourceId,
				ArrayList<QuestionAvailableOutputDTO> items) {
			super(context, textViewResourceId, items);
			this.questions = items;
			this.context = context;
			this.allAnswers = countAllAnswers(items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getApplication()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.question_list_item, null);
			}

			if(countAnswers < allAnswers) {
				
				QuestionAvailableOutputDTO question = questions.get(position);
				TextView tt = (TextView) v.findViewById(R.id.toptext);
				tt.setText(question.getQuestion());

				TextView questionIdTextView = (TextView) v.findViewById(R.id.questionId);
				questionIdTextView.setText(question.getId());
				
				RadioGroup rgroup = (RadioGroup) v
						.findViewById(R.id.optionRadioGroup);
				
				Button answerBut = (Button) v.findViewById(R.id.answer_but);
				answerBut.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						View parent = (View) v.getParent();
						if(parent != null) {
							
							RadioGroup radioGroup = (RadioGroup) parent
									.findViewById(R.id.optionRadioGroup);
							
							int selectedId = radioGroup.getCheckedRadioButtonId();

							if(selectedId != -1) {
					            // find the radiobutton by returned id
					            RadioButton selectedRB = (RadioButton) findViewById(selectedId);
					            
					            String answerId = answersIds.get(selectedRB.getId());    
					            
								String questionId = ((TextView) parent
									.findViewById(R.id.questionId)).getText()
									.toString();
								
								//Toast.makeText(context, questionId + " answerId: " + answerId, Toast.LENGTH_LONG).show();
								AnswerQuestionsTask answerTask = new AnswerQuestionsTask(
										context, questionId, answerId);
								answerTask.execute();
							} else {
								Toast.makeText(context, "Escolha uma op��o.", Toast.LENGTH_LONG).show();
							}
						}
					}
				});
				
				for (AnswerOutputDTO answer : question.getAnswers()) {
					RadioButton rb = new RadioButton(v.getContext());
					rb.setText(answer.getAnswer());
					rb.setId(countAnswers);
					answersIds.put(countAnswers, answer.getId());
					rb.setTextColor(getResources().getColor(R.color.main_purple));
					rgroup.addView(rb);
					if (question.getUserAnswer() != null
							&& question.getUserAnswer().equals(answer.getId())) {
						rb.setChecked(true);
					}
	
					countAnswers++;
				}
				return v;
			}

			return v;
		}

	}

	public int countAllAnswers(ArrayList<QuestionAvailableOutputDTO> questions) {
		
		int finalCount = 0;
		if(questions != null) {
			for(QuestionAvailableOutputDTO question : questions) {
				finalCount += question.getAnswers().size();
			}
			
			return finalCount;
		}
		
		return -1;
	}
	
	class AnswerQuestionsTask extends AsyncTask<String, String, String> {

		private Context context;

		private String questionId;

		private String answerId;

		public AnswerQuestionsTask(Context context, String questionId,
				String answerId) {
			this.context = context;
			this.questionId = questionId;
			this.answerId = answerId;
		}

		@Override
		protected void onPreExecute() {
			ConnectionHelper.checkInternetConnection(context);
		}

		@Override
		protected String doInBackground(String... arg0) {

			InputStream inputStream = null;
			String result = null;
			try {
				String url = Constants.SERVER_URL
						+ Constants.URL_ANSWER_QUESTION;

				SharedPreferences prefs = context.getSharedPreferences(
						"com.democracy", Context.MODE_PRIVATE);
				String token = prefs.getString(Constants.TOKEN_SP_KEY, null);

				HashMap<String, String> postDataParams = new HashMap<String, String>();
				postDataParams.put("questionId", this.questionId);
				postDataParams.put("answerId", this.answerId);
				postDataParams.put("token", token);

				HttpURLConnection conn = ConnectionHelper.getConnectionPost(url,
						"POST");

				OutputStream os = conn.getOutputStream();
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(os, "UTF-8"));
				writer.write(ConnectionHelper.getPostDataString(postDataParams));

				writer.flush();
				writer.close();
				os.close();

				int statusCode = conn.getResponseCode();

				/* 200 represents HTTP OK */
				if (statusCode == 200) {
					inputStream = new BufferedInputStream(conn.getInputStream());
					result = ConnectionHelper
							.convertInputStreamToString(inputStream);
				} else {
					result = null; // "Failed to fetch data!";
				}

				return result;
			} catch (Exception e) {
				return new String("Exception: " + e.getMessage());
			}
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(context, "Pergunta respondida.", Toast.LENGTH_LONG).show();
			
			System.out.println("ople");
			// create adapter
		}

	}
	//
	// class MakeCommentTask extends AsyncTask<String, String, String> {
	//
	// private Context context;
	//
	// private String questionId;
	//
	// private String comment;
	//
	// public MakeCommentTask(Context context, String questionId,
	// String comment) {
	// this.context = context;
	// this.questionId = questionId;
	// this.comment = comment;
	// }
	//
	// @Override
	// protected void onPreExecute() {
	// ConnectionHelper.checkInternetConenction(context);
	// }
	//
	// @Override
	// protected String doInBackground(String... arg0) {
	//
	// InputStream inputStream = null;
	// String result = null;
	// try {
	// String url = Constants.SERVER_URL + Constants.URL_MAKE_COMMENT;
	//
	// SharedPreferences prefs = context.getSharedPreferences(
	// "com.democracy", Context.MODE_PRIVATE);
	// String token = prefs.getString(Constants.TOKEN_SP_KEY, null);
	//
	// HashMap<String, String> postDataParams = new HashMap<String, String>();
	// postDataParams.put("questionId", this.questionId);
	// postDataParams.put("comment", this.comment);
	// postDataParams.put("token", token);
	//
	// HttpURLConnection conn = ConnectionHelper.getConnection(url,
	// "POST");
	//
	// OutputStream os = conn.getOutputStream();
	// BufferedWriter writer = new BufferedWriter(
	// new OutputStreamWriter(os, "UTF-8"));
	// writer.write(ConnectionHelper.getPostDataString(postDataParams));
	//
	// writer.flush();
	// writer.close();
	// os.close();
	//
	// int statusCode = conn.getResponseCode();
	//
	// /* 200 represents HTTP OK */
	// if (statusCode == 200) {
	// inputStream = new BufferedInputStream(conn.getInputStream());
	// result = ConnectionHelper
	// .convertInputStreamToString(inputStream);
	// } else {
	// result = null; // "Failed to fetch data!";
	// }
	//
	// return result;
	// } catch (Exception e) {
	// return new String("Exception: " + e.getMessage());
	// }
	// }
	//
	// @Override
	// protected void onPostExecute(String result) {
	// // Gson gson = new Gson();
	// // List<QuestionAvailableOutputDTO> questions =
	// // gson.fromJson(result,
	// // new TypeToken<List<QuestionAvailableOutputDTO>>() {
	// // }.getType());
	// System.out.println("ople");
	// // create adapter
	// }
	//
	// }

}
