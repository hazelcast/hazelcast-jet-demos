package com.rlab.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.rlab.cache.ContractInfoRepository;
import com.rlab.cache.CustomerUsageRepository;
import com.rlab.entity.ContractInfo;
import com.rlab.entity.CustomerUsageDetails;

import com.rlab.jet.connectivity.JetIntegrator;
import com.rlab.kafka.message.KMessage;

/**
 * 
 * @author Riaz Mohammed
 *
 */

@Controller
public class TeleCustomerController {
	
	@Autowired
	ContractInfoRepository  ciRep;
	
	@Autowired
	CustomerUsageRepository cuRep;
	
	@Autowired
	JetIntegrator jetI;
	

	@RequestMapping(value="/churnpredictor/{key}", method=RequestMethod.GET)
	public String churnpredictor(@PathVariable String key, Model model) {
		ContractInfo ci = ciRep.findByKey(key);
		CustomerUsageDetails cud = cuRep.findByKey(key);
		
		model.addAttribute("contractInfo", ci);
        model.addAttribute("customerUsage", cud);
        
        
        try {
			String churn = "1"  ; 
			// ApamaIntegrator.predictChurn(ci,cud);
			KMessage res = jetI.predictChurn(ci, cud);
			  model.addAttribute("churn", res.getAttribute("Result"));
			  model.addAttribute("P0", res.getAttribute("P0")+"%");
			  model.addAttribute("P1", res.getAttribute("P1")+"%");
			  
			  System.out.println( "WEB MOdel "+model);
			  if(churn.equals("1"))
				  model.addAttribute("churnMessage", "CUSTOMER LIKELY TO CHURN");
			  else
				  model.addAttribute("churnMessage", "CUSTOMER NOT LIKELY TO CHURN");
			  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return "churnpredictor";
	}
	
	 @RequestMapping(value="/churnpredictorMain",method=RequestMethod.GET)
		public String churnpredictorMain(Model model) {
		    return "churnpredictorMain";
		}
	  
	 @RequestMapping(value="/churnpredictorMain",method=RequestMethod.POST)
		public String basketsAdd(@RequestParam String areaCode,String phone, Model model) {
		 
		 ContractInfo ci = ciRep.findByKey(areaCode+"-"+phone);
		    model.addAttribute("contractInfo", ci);
	        return "redirect:/churnpredictor/" + ci.getKey();
		}
	
	/*
    @RequestMapping(value="/basket/{id}/items", method=RequestMethod.POST)
	public String basketsAddSkill(@PathVariable Long id, @RequestParam Long itemId, Model model) {
    	Item item = itemSvc.findOne(itemId);
    	Basket basket = basketSvc.findOne(id);

    	if (basket != null) {
    		if (!basket.hasItem(item)) {
    			//basket.getItems().add(item);
    			basket.addItem(item);
    		}
    		basketSvc.save(basket);
            model.addAttribute("basket", basketSvc.findOne(id));
            model.addAttribute("items", itemSvc.findAll());
            return "redirect:/basket/" + basket.getId();
    	}

        model.addAttribute("baskets", basketSvc.findAll());
        return "redirect:/baskets";
    } */
    
 

}
